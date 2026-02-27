import os
import sys
import gzip
import requests
import logging
import time
from datetime import datetime

# ============================================================
# CONFIGURATION
# ============================================================

TIMEOUT_SECONDS = 900
LOCAL_DIR = "/home/jsa/scraping"
HDFS_BASE_DIR = "/data/raw/tenants-jsa"
LOG_DIR = "/home/jsa/scraping/logs"

os.makedirs(LOCAL_DIR, exist_ok=True)
os.makedirs(LOG_DIR, exist_ok=True)

# ============================================================
# LOGGING CONFIGURATION
# ============================================================

log_filename = os.path.join(
    LOG_DIR,
    f"tenants_scraper_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s",
    handlers=[
        logging.FileHandler(log_filename),
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger()

# ============================================================
# DATASETS
# ============================================================

DATASETS = [
    {
        "name": "deces_france",
        "type": "incremental",
        "dataset_id": "liste-des-personnes-decedees-en-france",
        "hdfs_dir": f"{HDFS_BASE_DIR}/deces"
    },
    {
        "name": "medecins",
        "type": "snapshot",
        "dataset_id": "medecins",
        "hdfs_dir": f"{HDFS_BASE_DIR}/medecins"
    },
    {
        "name": "annuaire_professionnels_sante",
        "type": "snapshot",
        "dataset_id": "annuaire-des-professionnels-de-sante",
        "hdfs_dir": f"{HDFS_BASE_DIR}/annuaire_pro"
    },
    {
        "name": "elections_europeennes_2019",
        "type": "full",
        "dataset_id": "resultats-elections-europeennes-2019-bureau-de-vote",
        "hdfs_dir": f"{HDFS_BASE_DIR}/elections_2019"
    },
    {
        "name": "presidentielle_2022",
        "type": "full",
        "dataset_id": "elections-france-presidentielles-2022-1er-tour-par-bureau-de-vote",
        "hdfs_dir": f"{HDFS_BASE_DIR}/presidentielle_2022"
    },
    {
        "name": "legislatives_2022",
        "type": "full",
        "dataset_id": "elections-france-legislatives-2022-1er-tour-par-bureau-de-vote",
        "hdfs_dir": f"{HDFS_BASE_DIR}/legislatives_2022"
    }
]

# ============================================================
# HELPERS
# ============================================================

def hdfs_mkdir(path):
    logger.info(f"Création du dossier HDFS : {path}")
    os.system(f"hdfs dfs -mkdir -p {path}")

def hdfs_put(local, remote):
    logger.info(f"Envoi du fichier vers HDFS : {remote}")
    os.system(f"hdfs dfs -put -f {local} {remote}")

# ============================================================
# INCREMENTAL
# ============================================================

def scrape_incremental(dataset):

    start_time = time.time()

    dataset_id = dataset["dataset_id"]
    hdfs_dir = dataset["hdfs_dir"]

    BASE_URL = f"https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/{dataset_id}/exports/csv"
    current_year = datetime.now().year

    CUT_OFF_YEAR = 2025

    # Test si le dossier HDFS existe
    first_run = os.system(f"hdfs dfs -test -d {hdfs_dir}") != 0

    # =========================
    # LOGIQUE INTELLIGENTE CORRIGÉE
    # =========================

    if first_run:
        logger.info("Mode : PREMIÈRE EXÉCUTION DÉTECTÉE (chargement historique complet)")

        start_year = 2020
        end_year = CUT_OFF_YEAR

    else:
        logger.info("Mode : EXÉCUTION INCRÉMENTALE DÉTECTÉE (chargement des nouvelles données)")

        last_year = get_last_year_from_hdfs(hdfs_dir)

        if last_year is None:
            start_year = CUT_OFF_YEAR + 1
        else:
            start_year = last_year + 1

        end_year = start_year

    # Sécurité : éviter de scraper le futur
    if start_year > current_year:
        logger.info("Aucune nouvelle donnée disponible pour le moment.")
        return

    url = (
        f"{BASE_URL}?where="
        f"death_date >= date'{start_year}-01-01' "
        f"AND death_date <= date'{end_year}-12-31'"
    )

    logger.info(f"URL utilisée pour le téléchargement : {url}")

    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
    local_file = os.path.join(
         LOCAL_DIR,
         f"{dataset_id}_{start_year}_{run_id}.csv.gz")

    logger.info("Début du téléchargement des données...")

    with requests.get(url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with gzip.open(local_file, "wt", encoding="utf-8") as f:
            for line in r.iter_lines(decode_unicode=True):
                if line:
                    f.write(line + "\n")

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"Taille du fichier téléchargé : {file_size:.2f} MB")

    hdfs_mkdir(hdfs_dir)

    hdfs_put(local_file, f"{hdfs_dir}/{os.path.basename(local_file)}")

    os.remove(local_file)

    logger.info(f"Temps d'exécution : {round(time.time() - start_time, 2)} secondes\n")

def get_last_year_from_hdfs(hdfs_dir):
    try:
        output = os.popen(f"hdfs dfs -ls {hdfs_dir}").read()
        years = []

        for line in output.split("\n"):
            if ".csv" in line:
                parts = line.split("_")
                for part in parts:
                    if part.isdigit() and len(part) == 4:
                        year = int(part[:4])
                        years.append(year)

        if years:
            return max(years)
        else:
            return None

    except:
        return None
# ============================================================
# SNAPSHOT
# ============================================================

def scrape_snapshot(dataset):

    start_time = time.time()

    dataset_id = dataset["dataset_id"]
    hdfs_dir = dataset["hdfs_dir"]

    metadata_url = f"https://public.opendatasoft.com/api/v2/catalog/datasets/{dataset_id}"
    export_url = f"https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/{dataset_id}/exports/csv"

    last_update_file = os.path.join(LOCAL_DIR, f"{dataset_id}_last_update.txt")

    logger.info("Vérification de la date de dernière mise à jour du dataset...")

    r = requests.get(metadata_url)
    r.raise_for_status()
    data = r.json()

    remote_modified = data["dataset"]["metas"]["default"]["modified"]

    local_modified = None
    if os.path.exists(last_update_file):
        with open(last_update_file, "r") as f:
            local_modified = f.read().strip()

    logger.info(f"Dernière mise à jour côté serveur : {remote_modified}")
    logger.info(f"Dernière mise à jour locale enregistrée : {local_modified}")

    if remote_modified == local_modified:
        logger.info("Aucune mise à jour détectée. Rien à télécharger.\n")
        return

    logger.info("Mise à jour détectée -> téléchargement du snapshot en cours...")

    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
    local_file = os.path.join(LOCAL_DIR, f"{dataset_id}_snapshot_{run_id}.csv")

    with requests.get(export_url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with open(local_file, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"Taille du fichier téléchargé : {file_size:.2f} MB")

    if file_size == 0:
        logger.info("Aucune nouvelle donnée pour cette période. Aucun envoi vers HDFS.")
        os.remove(local_file)
        return

    hdfs_mkdir(hdfs_dir)
    hdfs_put(local_file, f"{hdfs_dir}/{os.path.basename(local_file)}")

    with open(last_update_file, "w") as f:
        f.write(remote_modified)

    os.remove(local_file)

    logger.info(f"Temps d'exécution : {round(time.time() - start_time, 2)} secondes\n")

# ============================================================
# FULL
# ============================================================

def scrape_full(dataset):

    start_time = time.time()

    dataset_id = dataset["dataset_id"]
    hdfs_dir = dataset["hdfs_dir"]

    url = f"https://public.opendatasoft.com/api/explore/v2.1/catalog/datasets/{dataset_id}/exports/csv"

    logger.info(f"Téléchargement complet du dataset : {dataset['name']}")
    logger.info(f"URL de téléchargement : {url}")

    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
    local_file = os.path.join(LOCAL_DIR, f"{dataset_id}_{run_id}.csv")

    with requests.get(url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with open(local_file, "wb") as f:
            for chunk in r.iter_content(chunk_size=8192):
                f.write(chunk)

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"Taille du fichier téléchargé : {file_size:.2f} MB")

    hdfs_mkdir(hdfs_dir)
    hdfs_put(local_file, f"{hdfs_dir}/{os.path.basename(local_file)}")

    os.remove(local_file)

    logger.info(f"Temps d'exécution : {round(time.time() - start_time, 2)} secondes\n")

# ============================================================
# MAIN
# ============================================================
def main():

    global_start = time.time()

    mode = sys.argv[1] if len(sys.argv) > 1 else "init"

    logger.info("=" * 80)
    logger.info(f"DÉMARRAGE DU SCRAPER TENANTS | MODE = {mode}")
    logger.info("=" * 80)

    if mode == "init":
        datasets_to_run = DATASETS

    elif mode == "deces":
        datasets_to_run = [d for d in DATASETS if d["type"] == "incremental"]

    elif mode == "sante":
        datasets_to_run = [d for d in DATASETS if d["type"] == "snapshot"]

    elif mode == "full":
        datasets_to_run = [d for d in DATASETS if d["type"] == "full"]

    else:
        logger.error("Use: init | deces | sante | full")
        sys.exit(1)

    for dataset in datasets_to_run:
        try:
            logger.info(f"Traitement du dataset : {dataset['name']}")

            if dataset["type"] == "incremental":
                scrape_incremental(dataset)

            elif dataset["type"] == "snapshot":
                scrape_snapshot(dataset)

            elif dataset["type"] == "full":
                scrape_full(dataset)

        except Exception as e:
            logger.error(f"Erreur lors du traitement du dataset {dataset['name']} : {str(e)}")

    logger.info("=" * 80)
    logger.info(f"TEMPS TOTAL D'EXÉCUTION : {round(time.time() - global_start, 2)} secondes")
    logger.info("FIN DU SCRAPER")
    logger.info("=" * 80)
if __name__ == "__main__":
    main()