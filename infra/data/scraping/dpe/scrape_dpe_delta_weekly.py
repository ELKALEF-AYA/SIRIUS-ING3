"""
DELTA weekly

HDFS target:
  /data/raw/dpe/data/dpe_delta_run-YYYY-MM-DD.jsonl.gz
  /data/raw/dpe/state/delta_last_run.json

Reads:
  /data/raw/dpe/state/full_state.json
"""

import os
import json
import time
import gzip
import shutil
import subprocess
from datetime import datetime, timezone
from urllib.parse import urlparse, parse_qs
import requests

# =========================
# CONFIG
# =========================

BASE_LINES_URL = "https://data.ademe.fr/data-fair/api/v1/datasets/dpe03existant/lines"
PAGE_SIZE = 10_000
SORT = "-date_derniere_modification_dpe,numero_dpe"

TIMEOUT_SECONDS = 180

# ADEME key
ADEME_API_KEY = os.getenv("ADEME_API_KEY")

LOCAL_TMP_DIR = "/tmp/dpe_delta_weekly"

HDFS_ROOT_DATA_DIR = "/data/raw/dpe/data"
HDFS_ROOT_STATE_DIR = "/data/raw/dpe/state"

HDFS_FULL_STATE_FILE = f"{HDFS_ROOT_STATE_DIR}/full_state.json"
HDFS_DELTA_LAST_RUN_FILE = f"{HDFS_ROOT_STATE_DIR}/delta_last_run.json"

# =========================
# TinyProxy
# =========================
PROXIES = {
    "http": "http://127.0.0.1:8888",
    "https": "http://127.0.0.1:8888",
}

# =========================
# HELPERS
# =========================

def utc_today():
    return datetime.now(timezone.utc).strftime("%Y-%m-%d")

def is_ge(a, b):
    if not a or not b:
        return False
    return a >= b

def run_cmd(cmd):
    p = subprocess.run(cmd, text=True, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(p.stderr)
    return p

def hdfs_read_json(path):
    p = subprocess.run(["hdfs", "dfs", "-cat", path], text=True, capture_output=True)
    if p.returncode != 0:
        return None
    return json.loads(p.stdout)

def hdfs_write_json(path, obj):
    os.makedirs(LOCAL_TMP_DIR, exist_ok=True)
    local = os.path.join(LOCAL_TMP_DIR, os.path.basename(path))
    with open(local, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    run_cmd(["hdfs", "dfs", "-mkdir", "-p", os.path.dirname(path)])
    run_cmd(["hdfs", "dfs", "-put", "-f", local, path])
    os.remove(local)

# =========================
# MAIN
# =========================

def main():

    run_date = utc_today()
    started_date = run_date

    os.makedirs(LOCAL_TMP_DIR, exist_ok=True)
    run_cmd(["hdfs", "dfs", "-mkdir", "-p", HDFS_ROOT_DATA_DIR])
    run_cmd(["hdfs", "dfs", "-mkdir", "-p", HDFS_ROOT_STATE_DIR])

    full_state = hdfs_read_json(HDFS_FULL_STATE_FILE)
    min_full = full_state["min_date_reception_in_full"]

    last = hdfs_read_json(HDFS_DELTA_LAST_RUN_FILE)
    if last and last.get("last_run_date"):
        since = last["last_run_date"]
    else:
        since = full_state["full_finished_date"]

    print("\n[MODE] DELTA weekly")
    print(f"[INFO] min_date_reception_in_full = {min_full}")
    print(f"[INFO] since_date = {since}")

    session = requests.Session()
    session.headers.update({"Accept": "application/json"})
    if ADEME_API_KEY:
        session.headers.update({"x-api-key": ADEME_API_KEY})

    session.proxies.update(PROXIES)

    after = None
    scanned = 0
    kept = 0
    kept_new = 0
    kept_modified = 0

    local_file = os.path.join(LOCAL_TMP_DIR, f"dpe_delta_run-{run_date}.jsonl.gz")
    gz = None

    while True:

        params = {"size": PAGE_SIZE, "sort": SORT}
        if after:
            params["after"] = after

        r = session.get(BASE_LINES_URL, params=params, timeout=TIMEOUT_SECONDS)
        r.raise_for_status()
        data = r.json()

        items = data.get("results", [])
        next_url = data.get("next")

        if not items:
            break

        scanned += len(items)

        dm_last = items[-1].get("date_derniere_modification_dpe")
        if dm_last and dm_last < since:
            break

        for rec in items:

            dr = rec.get("date_reception_dpe")
            dm = rec.get("date_derniere_modification_dpe")

            if not is_ge(dr, min_full):
                continue

            if not is_ge(dm, since):
                continue

            if gz is None:
                gz = gzip.open(local_file, "wt", encoding="utf-8")

            gz.write(json.dumps(rec, ensure_ascii=False))
            gz.write("\n")

            kept += 1

            if is_ge(dr, since):
                kept_new += 1
            else:
                kept_modified += 1

        if next_url:
            qs = parse_qs(urlparse(next_url).query)
            after = qs.get("after", [None])[0]
        else:
            break

    if gz:
        gz.close()

    # Écriture delta
    if kept > 0:
        hdfs_path = f"{HDFS_ROOT_DATA_DIR}/dpe_delta_run-{run_date}.jsonl.gz"
        run_cmd(["hdfs", "dfs", "-put", "-f", local_file, hdfs_path])
        os.remove(local_file)
        print(f"[OK] Delta écrit : {hdfs_path}")
    else:
        if os.path.exists(local_file):
            os.remove(local_file)
        print(f"[INFO] Aucun enregistrement nouveau ou modifié depuis le dernier run {since}")

    # Mise à jour état
    hdfs_write_json(HDFS_DELTA_LAST_RUN_FILE, {
        "last_run_date": run_date,
        "started_date_utc": started_date,
        "finished_date_utc": utc_today(),
        "since_date": since,
        "min_date_reception_in_full": min_full,
        "scanned_records": scanned,
        "kept_records": kept,
        "kept_new_records": kept_new,
        "kept_modified_records": kept_modified
    })

    print("\n[RESULTATS]")
    print(f"scanned = {scanned}")
    print(f"kept = {kept}")
    print(f"kept_new = {kept_new}")
    print(f"kept_modified = {kept_modified}")
    print("[DONE] DELTA\n")

    try:
        if not os.listdir(LOCAL_TMP_DIR):
            shutil.rmtree(LOCAL_TMP_DIR)
    except:
        pass


if __name__ == "__main__":
    main()