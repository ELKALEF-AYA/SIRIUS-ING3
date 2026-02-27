"""
FULL

HDFS target:
  /data/raw/dpe/data/dpe_full_run-YYYY-MM-DD_part-00000.jsonl.gz
  /data/raw/dpe/state/full_state.json
  /data/raw/dpe/state/_SUCCESS
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
SORT = "-date_reception_dpe,numero_dpe"

TARGET_TOTAL_LINES = 4_000_000
LINES_PER_PART = 1_000_000

MAX_RETRIES = 5

# ADEME key
ADEME_API_KEY = os.getenv("ADEME_API_KEY")

# Local temp directory
LOCAL_TMP_DIR = "/tmp/dpe_full_4m"

# HDFS target
HDFS_FULL_DATA_DIR = "/data/raw/dpe/data"
HDFS_FULL_STATE_DIR = "/data/raw/dpe/state"
HDFS_FULL_STATE_FILE = f"{HDFS_FULL_STATE_DIR}/full_state.json"
HDFS_SUCCESS_FILE = f"{HDFS_FULL_STATE_DIR}/_SUCCESS"

# =========================
# TinyProxy
# =========================
PROXIES = {
    "http": "http://127.0.0.1:8888",
    "https": "http://127.0.0.1:8888",
}

# =========================
# Session
# =========================

ADEME = requests.Session()
ADEME.headers.update({"Accept": "application/json"})
if ADEME_API_KEY:
    ADEME.headers.update({"x-api-key": ADEME_API_KEY})

ADEME.proxies.update(PROXIES)

# =========================
# Helpers
# =========================

def utc_today() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%d")

def extract_results(payload: dict) -> list:
    if isinstance(payload, dict) and isinstance(payload.get("results"), list):
        return payload["results"]
    return []

def get_next_url(payload: dict) -> str | None:
    if isinstance(payload, dict):
        return payload.get("next")
    return None

def extract_after_from_next(next_url: str) -> str | None:
    try:
        qs = parse_qs(urlparse(next_url).query)
        return qs.get("after", [None])[0]
    except Exception:
        return None

def make_part_filename(run_date: str, part_index: int) -> str:
    return f"dpe_full_run-{run_date}_part-{part_index:05d}.jsonl.gz"

def request_json(params: dict) -> dict:
    last_err = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            r = ADEME.get(
                BASE_LINES_URL,
                params=params,
                timeout=180,
            )
            if r.status_code in (429, 500, 502, 503, 504):
                time.sleep(min(2 ** attempt, 30))
                continue
            r.raise_for_status()
            return r.json()
        except Exception as e:
            last_err = e
            if attempt == MAX_RETRIES:
                raise
            time.sleep(min(2 ** attempt, 30))
    raise last_err

def run_cmd(cmd: list[str]) -> subprocess.CompletedProcess:
    p = subprocess.run(cmd, text=True, capture_output=True)
    if p.returncode != 0:
        raise RuntimeError(
            f"Command failed: {' '.join(cmd)}\n"
            f"stdout:\n{p.stdout}\n"
            f"stderr:\n{p.stderr}\n"
        )
    return p

def hdfs_mkdir_p(path: str) -> None:
    run_cmd(["hdfs", "dfs", "-mkdir", "-p", path])

def hdfs_put_overwrite(local_file: str, hdfs_dir_or_file: str) -> None:
    run_cmd(["hdfs", "dfs", "-put", "-f", local_file, hdfs_dir_or_file])

def hdfs_test_exists(hdfs_path: str) -> bool:
    p = subprocess.run(["hdfs", "dfs", "-test", "-e", hdfs_path])
    return p.returncode == 0

def hdfs_touchz(hdfs_path: str) -> None:
    run_cmd(["hdfs", "dfs", "-touchz", hdfs_path])

def hdfs_put_state_json(hdfs_state_path: str, obj: dict) -> None:
    local_state = os.path.join(LOCAL_TMP_DIR, "full_state.json")
    with open(local_state, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)
        f.write("\n")

    hdfs_mkdir_p(os.path.dirname(hdfs_state_path))

    hdfs_put_overwrite(local_state, hdfs_state_path)

    os.remove(local_state)

# =========================
# PART writer
# =========================

def write_one_part_local(run_date: str, part_index: int, after_start: str | None, remaining_total: int):
    limit = min(LINES_PER_PART, remaining_total)
    after = after_start
    lines_done = 0
    min_date_seen = None

    local_name = make_part_filename(run_date, part_index)
    local_path = os.path.join(LOCAL_TMP_DIR, local_name)

    with gzip.open(local_path, "wt", encoding="utf-8", compresslevel=6) as gz:
        while True:
            if lines_done >= limit:
                return after, lines_done, False, local_path, min_date_seen

            params = {"size": PAGE_SIZE, "sort": SORT}
            if after:
                params["after"] = after

            data = request_json(params)
            items = extract_results(data)
            next_url = get_next_url(data)

            if not items:
                return after, lines_done, True, local_path, min_date_seen

            for item in items:
                gz.write(json.dumps(item, ensure_ascii=False))
                gz.write("\n")

                d = item.get("date_reception_dpe")
                if d and ((min_date_seen is None) or (d < min_date_seen)):
                    min_date_seen = d

            lines_done += len(items)
            if lines_done % 100000 == 0:
                print(f"[INFO] part_lines={lines_done}", flush=True)

            if next_url:
                after = extract_after_from_next(next_url)
            else:
                return after, lines_done, True, local_path, min_date_seen

            if len(items) < PAGE_SIZE:
                return after, lines_done, True, local_path, min_date_seen

# =========================
# MAIN
# =========================

def main():
    if hdfs_test_exists(HDFS_SUCCESS_FILE):
        print("[SKIP] le scraping est déjà fait", flush=True)
        return

    started_date = utc_today()
    print(f"[MODE] FULL -> RAW (target={TARGET_TOTAL_LINES})", flush=True)

    os.makedirs(LOCAL_TMP_DIR, exist_ok=True)

    hdfs_mkdir_p(HDFS_FULL_DATA_DIR)
    hdfs_mkdir_p(HDFS_FULL_STATE_DIR)

    total_written = 0
    part_index = 0
    after = None

    min_date_global = None

    while total_written < TARGET_TOTAL_LINES:
        remaining = TARGET_TOTAL_LINES - total_written

        print(f"[INFO] Création part {part_index:05d}", flush=True)

        after_end, lines_written, ended, local_file, min_date_part = write_one_part_local(
            run_date=started_date,
            part_index=part_index,
            after_start=after,
            remaining_total=remaining,
        )

        if min_date_part and ((min_date_global is None) or (min_date_part < min_date_global)):
            min_date_global = min_date_part

        print(f"[INFO] Écriture part {part_index:05d} -> HDFS", flush=True)
        hdfs_put_overwrite(local_file, HDFS_FULL_DATA_DIR)

        os.remove(local_file)

        after = after_end
        total_written += lines_written

        print(f"[OK] Part {part_index:05d}: lines={lines_written}, total={total_written}", flush=True)

        if lines_written == 0 and ended:
            break
        if ended:
            break

        part_index += 1

    finished_date = utc_today()

    state_obj = {
        "full_started_date": started_date,
        "full_finished_date": finished_date,
        "min_date_reception_in_full": min_date_global,
    }

    hdfs_put_state_json(HDFS_FULL_STATE_FILE, state_obj)

    hdfs_touchz(HDFS_SUCCESS_FILE)

    try:
        if not os.listdir(LOCAL_TMP_DIR):
            shutil.rmtree(LOCAL_TMP_DIR)
    except Exception:
        pass

    print(f"[OK] full_state.json -> {HDFS_FULL_STATE_FILE}", flush=True)
    print(f"[OK] SUCCESS -> {HDFS_SUCCESS_FILE}", flush=True)
    print("[DONE] FULL", flush=True)

if __name__ == "__main__":
    main()