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
HDFS_TARGET_DIR = "/data/raw/Score_Payment"
LOG_DIR = "/home/jsa/scraping/logs"

os.makedirs(LOCAL_DIR, exist_ok=True)
os.makedirs(LOG_DIR, exist_ok=True)

# ============================================================
# LOGGING
# ============================================================

log_filename = os.path.join(
    LOG_DIR,
    f"score_payment_scraper_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
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

# Sources statiques (figees, scrape unique)
DATASETS_FULL = [
    {
        "name": "elections_regionales_2021_2nd_tour",
        "type": "full",
        "csv_url": (
            "https://public.opendatasoft.com/api/explore/v2.1/"
            "catalog/datasets/elections-france-regionales-2021-2nd-tour-par-bureau-de-vote/exports/csv"
        )
    },
    {
        "name": "elections_municipales_2020_1er_tour",
        "type": "full",
        "csv_url": (
            "https://public.opendatasoft.com/api/explore/v2.1/"
            "catalog/datasets/election-france-municipale-2020-premier-tour/exports/csv"
        )
    },
    {
        "name": "elections_europeennes_2019",
        "type": "full",
        "csv_url": (
            "https://public.opendatasoft.com/api/explore/v2.1/"
            "catalog/datasets/resultats-elections-europeennes-2019-bureau-de-vote/exports/csv"
        )
    },
    {
        "name": "indice_defavorisation_sociale_fdep",
        "type": "full",
        "csv_url": (
            "https://public.opendatasoft.com/api/explore/v2.1/"
            "catalog/datasets/indice-de-defavorisation-sociale-fdep-par-iris/exports/csv"
        )
    },
    {
        "name": "fiscalite_locale_communes",
        "type": "full",
        "csv_url": (
            "https://public.opendatasoft.com/api/explore/v2.1/"
            "catalog/datasets/economicref-france-commune-fiscalite-locale/exports/csv"
        )
    },
    {
        "name": "scores_multiexposition_environnement_idf",
        "type": "full",
        "csv_url": (
            "https://data.smartidf.services/api/explore/v2.1/"
            "catalog/datasets/scores-multiexposition-environnementale-ile-de-france-grille-500m/exports/csv"
        )
    }
]

# Source annuelle (colleges) - mise a jour annuelle officielle
DATASETS_SNAPSHOT = [
    {
        "name": "indicateurs_valeur_ajoutee_colleges",
        "type": "snapshot",
        "dataset_id": "fr-en-indicateurs-valeur-ajoutee-colleges",
        "base_url": "https://data.smartidf.services",
        "csv_url": (
            "https://data.smartidf.services/api/explore/v2.1/"
            "catalog/datasets/fr-en-indicateurs-valeur-ajoutee-colleges/exports/csv"
        )
    }
]

# Source incrementale (carburants) - mise a jour quotidienne
DATASETS_INCREMENTAL = [
    {
        "name": "prix_carburants",
        "type": "incremental",
        "dataset_id": "prix-des-carburants-j-1",
        "base_url": "https://public.opendatasoft.com",
        "date_field": "update",
        "hdfs_dir": f"{HDFS_TARGET_DIR}/carburants"
    }
]

# ============================================================
# HELPERS
# ============================================================

def hdfs_mkdir(path):
    logger.info(f"Creating HDFS directory: {path}")
    os.system(f"hdfs dfs -mkdir -p {path}")

def hdfs_put(local, remote):
    logger.info(f"Uploading to HDFS: {remote}")
    ret = os.system(f"hdfs dfs -put -f {local} {remote}")
    if ret != 0:
        raise RuntimeError(f"HDFS put failed: {local} -> {remote}")

def get_last_date_from_hdfs(hdfs_dir):
    """
    Lit les fichiers dans HDFS et retourne la derniere annee trouvee
    dans les noms de fichiers pour determiner le point de reprise
    """
    try:
        output = os.popen(f"hdfs dfs -ls {hdfs_dir}").read()
        years = []
        for line in output.split("\n"):
            if ".csv" in line:
                parts = line.split("_")
                for part in parts:
                    clean = part.replace(".csv.gz", "").replace(".csv", "")
                    if clean.isdigit() and len(clean) == 4:
                        years.append(int(clean))
        return max(years) if years else None
    except Exception:
        return None

# ============================================================
# SCRAPING FULL (sources statiques figees)
# ============================================================

def scrape_full(dataset):
    """
    Telecharge la totalite du dataset en une seule fois.
    Utilise pour les sources historiques qui ne seront jamais mises a jour.
    """
    start_time = time.time()
    name = dataset["name"]
    csv_url = dataset["csv_url"]
    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")

    local_file = os.path.join(LOCAL_DIR, f"{name}_raw_{run_id}.csv.gz")
    hdfs_target = f"{HDFS_TARGET_DIR}/{os.path.basename(local_file)}"

    logger.info("=" * 80)
    logger.info(f"[FULL] Dataset     : {name}")
    logger.info(f"[FULL] Source URL  : {csv_url}")
    logger.info(f"[FULL] HDFS target : {hdfs_target}")

    with requests.get(csv_url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with gzip.open(local_file, "wt", encoding="utf-8") as f:
            row_count = 0
            for line in r.iter_lines(decode_unicode=True):
                if line:
                    f.write(line + "\n")
                    row_count += 1

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"[FULL] Rows downloaded: {row_count - 1}")
    logger.info(f"[FULL] File size: {file_size:.2f} MB")

    hdfs_mkdir(HDFS_TARGET_DIR)
    hdfs_put(local_file, hdfs_target)

    os.remove(local_file)
    logger.info(f"[FULL] Local file deleted")
    logger.info(f"[FULL] Execution time: {round(time.time() - start_time, 2)}s")

# ============================================================
# SCRAPING SNAPSHOT (source colleges - annuelle)
# ============================================================

def scrape_snapshot(dataset):
    """
    Verifie si la source a ete mise a jour depuis le dernier scrape.
    Si oui, telecharge la nouvelle version complete.
    Utilise pour les sources a mise a jour annuelle (colleges IVAC).
    """
    start_time = time.time()
    name = dataset["name"]
    dataset_id = dataset["dataset_id"]
    base_url = dataset["base_url"]
    csv_url = dataset["csv_url"]

    metadata_url = f"{base_url}/api/explore/v2.1/catalog/datasets/{dataset_id}"
    last_update_file = os.path.join(LOCAL_DIR, f"{name}_last_update.txt")

    logger.info("=" * 80)
    logger.info(f"[SNAPSHOT] Dataset: {name}")
    logger.info(f"[SNAPSHOT] Checking metadata for update...")

    r = requests.get(metadata_url, timeout=60)
    r.raise_for_status()
    data = r.json()

    remote_modified = data.get("dataset", {}).get("metas", {}).get("default", {}).get("modified", "")

    local_modified = None
    if os.path.exists(last_update_file):
        with open(last_update_file, "r") as f:
            local_modified = f.read().strip()

    logger.info(f"[SNAPSHOT] Remote last update : {remote_modified}")
    logger.info(f"[SNAPSHOT] Local last update  : {local_modified}")

    if remote_modified == local_modified:
        logger.info(f"[SNAPSHOT] No update detected - skipping download")
        return

    logger.info(f"[SNAPSHOT] Update detected - downloading new version")

    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
    local_file = os.path.join(LOCAL_DIR, f"{name}_snapshot_{run_id}.csv.gz")
    hdfs_target = f"{HDFS_TARGET_DIR}/{os.path.basename(local_file)}"

    with requests.get(csv_url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with gzip.open(local_file, "wt", encoding="utf-8") as f:
            row_count = 0
            for line in r.iter_lines(decode_unicode=True):
                if line:
                    f.write(line + "\n")
                    row_count += 1

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"[SNAPSHOT] Rows downloaded: {row_count - 1}")
    logger.info(f"[SNAPSHOT] File size: {file_size:.2f} MB")

    hdfs_mkdir(HDFS_TARGET_DIR)
    hdfs_put(local_file, hdfs_target)

    with open(last_update_file, "w") as f:
        f.write(remote_modified)

    os.remove(local_file)
    logger.info(f"[SNAPSHOT] Local file deleted")
    logger.info(f"[SNAPSHOT] Execution time: {round(time.time() - start_time, 2)}s")

# ============================================================
# SCRAPING INCREMENTAL (source carburants - quotidienne)
# ============================================================

def scrape_incremental(dataset):
    """
    Logique incrementale avec detection automatique du run :
    - Premier run (HDFS vide) : recupere uniquement decembre 2025
    - Runs suivants (HDFS peuple) : recupere uniquement les nouvelles
      donnees a partir de 2026 (delta uniquement)
    Utilise pour la source prix carburants mise a jour quotidiennement.
    """
    start_time = time.time()
    name = dataset["name"]
    dataset_id = dataset["dataset_id"]
    base_url = dataset["base_url"]
    date_field = dataset["date_field"]
    hdfs_dir = dataset["hdfs_dir"]

    CUT_OFF_YEAR = 2025
    current_year = datetime.now().year

    BASE_URL = (
        f"{base_url}/api/explore/v2.1/"
        f"catalog/datasets/{dataset_id}/exports/csv"
    )

    logger.info("=" * 80)
    logger.info(f"[INCREMENTAL] Dataset: {name}")

    # Detection du type de run
    first_run = os.system(f"hdfs dfs -test -d {hdfs_dir}") != 0

    if first_run:
        # Premier run : on recupere uniquement decembre 2025 (bootstrap leger)
        logger.info(f"[INCREMENTAL] Mode: FIRST RUN - recuperation decembre {CUT_OFF_YEAR} uniquement")
        start_date = f"{CUT_OFF_YEAR}-12-01"
        end_date = f"{CUT_OFF_YEAR}-12-31"
        year_label = str(CUT_OFF_YEAR)
    else:
        # Runs suivants : on recupere uniquement les nouvelles donnees (2026+)
        logger.info(f"[INCREMENTAL] Mode: INCREMENTAL RUN - recuperation nouvelles donnees")
        last_year = get_last_date_from_hdfs(hdfs_dir)

        if last_year is None:
            start_year = CUT_OFF_YEAR + 1
        else:
            start_year = last_year + 1

        if start_year > current_year:
            logger.info(f"[INCREMENTAL] Aucune nouvelle donnee disponible pour l'instant")
            return

        start_date = f"{start_year}-01-01"
        end_date = f"{start_year}-12-31"
        year_label = str(start_year)

    # Construction de l'URL avec filtre sur le champ date
    url = (
        f"{BASE_URL}?where="
        f"{date_field}>=date'{start_date}'"
        f"%20AND%20"
        f"{date_field}<=date'{end_date}'"
    )

    logger.info(f"[INCREMENTAL] Periode : {start_date} -> {end_date}")
    logger.info(f"[INCREMENTAL] URL     : {url}")

    run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
    local_file = os.path.join(LOCAL_DIR, f"{name}_{year_label}_{run_id}.csv.gz")
    hdfs_target = f"{hdfs_dir}/{os.path.basename(local_file)}"

    with requests.get(url, stream=True, timeout=TIMEOUT_SECONDS) as r:
        r.raise_for_status()
        with gzip.open(local_file, "wt", encoding="utf-8") as f:
            row_count = 0
            for line in r.iter_lines(decode_unicode=True):
                if line:
                    f.write(line + "\n")
                    row_count += 1

    file_size = os.path.getsize(local_file) / (1024 * 1024)
    logger.info(f"[INCREMENTAL] Rows downloaded: {row_count - 1}")
    logger.info(f"[INCREMENTAL] File size: {file_size:.2f} MB")

    if row_count <= 1:
        logger.info(f"[INCREMENTAL] Aucune donnee pour cette periode - fichier ignore")
        os.remove(local_file)
        return

    hdfs_mkdir(hdfs_dir)
    hdfs_put(local_file, hdfs_target)

    os.remove(local_file)
    logger.info(f"[INCREMENTAL] Local file deleted")
    logger.info(f"[INCREMENTAL] Execution time: {round(time.time() - start_time, 2)}s")

# ============================================================
# MAIN
# ============================================================

def main():
    global_start = time.time()

    mode = sys.argv[1] if len(sys.argv) > 1 else "full"

    logger.info("=" * 80)
    logger.info(f"SCORE PAYMENT SCRAPER STARTED | MODE = {mode}")
    logger.info("=" * 80)

    if mode == "full":
        # Scrape des 6 sources statiques figees
        for dataset in DATASETS_FULL:
            try:
                logger.info(f"Processing: {dataset['name']}")
                scrape_full(dataset)
            except Exception as e:
                logger.error(f"Error on {dataset['name']}: {str(e)}")

    elif mode == "colleges":
        # Scrape annuel des colleges IVAC (snapshot)
        for dataset in DATASETS_SNAPSHOT:
            try:
                logger.info(f"Processing: {dataset['name']}")
                scrape_snapshot(dataset)
            except Exception as e:
                logger.error(f"Error on {dataset['name']}: {str(e)}")

    elif mode == "carburants":
        # Scrape incremental des prix carburants (quotidien)
        for dataset in DATASETS_INCREMENTAL:
            try:
                logger.info(f"Processing: {dataset['name']}")
                scrape_incremental(dataset)
            except Exception as e:
                logger.error(f"Error on {dataset['name']}: {str(e)}")

    else:
        logger.error("Mode invalide. Utiliser: full | colleges | carburants")
        sys.exit(1)

    logger.info("=" * 80)
    logger.info(f"TOTAL EXECUTION TIME: {round(time.time() - global_start, 2)} seconds")
    logger.info("SCRAPER FINISHED")
    logger.info("=" * 80)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        logger.info("Interrupted by user")
        sys.exit(130)
    except Exception as e:
        logger.error(f"Error: {e}")
        sys.exit(1)