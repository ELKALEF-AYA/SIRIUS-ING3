import os
import sys
import json
import time
import gzip
import requests
from datetime import datetime
from urllib.parse import urljoin

# -----------------------------
# Configuration
# -----------------------------
TIMEOUT_SECONDS = 60
SLEEP_BETWEEN_CALLS_SECONDS = 0.2
MAX_RETRIES = 5

PAGE_SIZE = 5000  # pagination API

LOCAL_DIR = "/home/jsa/scraping"
RUN_ID = datetime.now().strftime("%Y%m%d_%H%M%S")

HDFS_RAW_DIR = "/data/raw/properties"

# Liste des départements (01..95 + 2A/2B)
DEPARTEMENTS = (
        [f"{i:02d}" for i in range(1, 96) if i != 20]  # 01..95 sans 20
        + ["2A", "2B"]
)

# -----------------------------
# Helpers
# -----------------------------
def http_get_json(url: str) -> dict:
    last_err = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            r = requests.get(url, timeout=TIMEOUT_SECONDS)
            if r.status_code != 200:
                raise RuntimeError(f"HTTP {r.status_code}: {r.text[:200]}")
            return r.json()
        except Exception as e:
            last_err = e
            wait = min(2 ** attempt, 20)
            print(f"Tentative {attempt}/{MAX_RETRIES} échouée : {e}. Nouvelle tentative dans {wait}s...")
            time.sleep(wait)
    raise RuntimeError(f"Échec après {MAX_RETRIES} tentatives : {last_err}")

def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)

def hdfs_mkdir(path: str) -> None:
    rc = os.system(f"hdfs dfs -mkdir -p {path}")
    if rc != 0:
        raise RuntimeError(f"Échec de création du dossier HDFS : {path} (code={rc})")

def hdfs_put(local_path: str, hdfs_path: str) -> None:
    rc = os.system(f"hdfs dfs -put -f {local_path} {hdfs_path}")
    if rc != 0:
        raise RuntimeError(f"Échec d'upload HDFS : {local_path} -> {hdfs_path} (code={rc})")

def hdfs_exists(hdfs_path: str) -> bool:
    rc = os.system(f"hdfs dfs -test -e {hdfs_path}")
    return rc == 0

def delete_local_file(path: str) -> None:
    try:
        os.remove(path)
        print(f"Fichier local supprimé : {path}")
    except FileNotFoundError:
        print(f"Fichier local introuvable (déjà supprimé) : {path}")
    except Exception as e:
        raise RuntimeError(f"Impossible de supprimer le fichier local {path} : {e}")

def build_api_start_url(dep: str) -> str:
    # Les datasets ADEME sont de la forme dpe-94, dpe-2a, dpe-2b
    dep_slug = dep.lower()
    return f"https://data.ademe.fr/data-fair/api/v1/datasets/dpe-{dep_slug}/lines?size={PAGE_SIZE}"

def download_department(dep: str) -> None:
    api_start_url = build_api_start_url(dep)

    local_file = os.path.join(LOCAL_DIR, f"dpe_{dep}_raw_{RUN_ID}.jsonl.gz")
    hdfs_file_name = f"dpe_{dep}_raw_{RUN_ID}.jsonl.gz"
    hdfs_target = f"{HDFS_RAW_DIR}/{hdfs_file_name}"

    print("------------------------------------------------------------")
    print(f"Département {dep} : démarrage")
    print(f"URL de départ : {api_start_url}")
    print(f"Sortie locale : {local_file}")
    print(f"Cible HDFS    : {hdfs_target}")

    url = api_start_url
    total_written = 0
    page = 0

    with gzip.open(local_file, "wt", encoding="utf-8") as f:
        while url:
            page += 1
            data = http_get_json(url)

            results = data.get("results", [])
            next_url = data.get("next")

            for row in results:
                f.write(json.dumps(row, ensure_ascii=False) + "\n")

            total_written += len(results)
            print(f"Département {dep} - Page {page} : +{len(results)} lignes (total={total_written})")

            if next_url:
                if next_url.startswith("/"):
                    url = urljoin("https://data.ademe.fr", next_url)
                else:
                    url = next_url
                time.sleep(SLEEP_BETWEEN_CALLS_SECONDS)
            else:
                url = None

    size_bytes = os.path.getsize(local_file)
    print(f"Département {dep} : fichier local créé ({size_bytes} octets)")

    # Upload HDFS
    hdfs_mkdir(HDFS_RAW_DIR)
    hdfs_put(local_file, hdfs_target)

    # Vérification avant suppression locale
    if not hdfs_exists(hdfs_target):
        raise RuntimeError(f"Département {dep} : fichier introuvable dans HDFS après upload. Suppression locale annulée.")

    print(f"Département {dep} : upload terminé, fichier disponible dans HDFS")
    delete_local_file(local_file)
    print(f"Département {dep} : terminé avec succès")

# -----------------------------
# Main
# -----------------------------
def main():
    ensure_dir(LOCAL_DIR)

    print("Démarrage de la récupération ADEME pour plusieurs départements")
    print(f"Dossier HDFS cible : {HDFS_RAW_DIR}")
    print(f"RUN_ID : {RUN_ID}")
    print(f"Nombre de départements : {len(DEPARTEMENTS)}")
    print("Liste : " + ", ".join(DEPARTEMENTS))

    ok = []
    ko = []

    for dep in DEPARTEMENTS:
        try:
            download_department(dep)
            ok.append(dep)
        except Exception as e:
            print(f"Département {dep} : échec -> {e}")
            ko.append(dep)

    print("------------------------------------------------------------")
    print("Résumé")
    print(f"Succès ({len(ok)}) : " + (", ".join(ok) if ok else "aucun"))
    print(f"Échecs ({len(ko)}) : " + (", ".join(ko) if ko else "aucun"))

    if ko:
        sys.exit(2)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("Interruption utilisateur")
        sys.exit(130)
    except Exception as e:
        print(f"Erreur : {e}")
        sys.exit(1)