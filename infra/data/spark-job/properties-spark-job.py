from pyspark.sql import SparkSession
from pyspark.sql import functions as F
import re

RAW_DIR = "/data/raw/properties"

CURATED_BASE_DIR = "/data/curated/properties"

# Pattern des fichiers attendus dans RAW
# Exemple: dpe_01_raw_20260129_182151.jsonl.gz, dpe_2A_raw_....jsonl.gz
PATTERN = re.compile(r"^dpe_(?P<dep>[0-9]{2}|2A|2B)_raw_.*\.jsonl\.gz$", re.IGNORECASE)

def list_raw_files(spark: SparkSession):
    jvm = spark._jvm
    hconf = spark._jsc.hadoopConfiguration()
    fs = jvm.org.apache.hadoop.fs.FileSystem.get(hconf)
    path = jvm.org.apache.hadoop.fs.Path(RAW_DIR)

    if not fs.exists(path):
        raise RuntimeError(f"Dossier RAW introuvable dans HDFS: {RAW_DIR}")

    statuses = fs.listStatus(path)
    files = []

    for st in statuses:
        if st.isFile():
            p = st.getPath()
            name = p.getName()

            # Format garanti: dpe_<dep>_raw_YYYYMMDD_HHMMSS.jsonl.gz
            # Exemple: dpe_01_raw_20260129_182151.jsonl.gz -> dep = "01"
            dep = name.split("_")[1].upper()

            files.append((dep, p.toString()))

    # Tri par dep puis par nom (timestamp) -> le dernier sera le plus récent
    files.sort(key=lambda x: (x[0], x[1]))
    return files

def transform(df):
    required = [
        "classe_consommation_energie",
        "tr001_modele_dpe_type_libelle",
        "tv016_departement_code",
        "tv016_departement_departement"
    ]

    # Vérifie que les colonnes attendues existent dans le RAW
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise RuntimeError(f"Colonnes manquantes dans le RAW : {missing}")

    curated = (
        # Sélection des colonnes utiles
        df.select(*required)

        # Renommage
        .withColumnRenamed("classe_consommation_energie", "classe_dpe")
        .withColumnRenamed("tr001_modele_dpe_type_libelle", "statut_bien")

        # Nettoyage / normalisation
        # - classe_dpe: trim + majuscule
        .withColumn("classe_dpe", F.upper(F.trim(F.col("classe_dpe"))))
        # - statut_bien: trim + format Titre (Location/Vente)
        .withColumn("statut_bien", F.initcap(F.trim(F.col("statut_bien"))))

        # Filtrage: on conserve uniquement les biens en Location
        .filter(F.col("statut_bien") == "Location")

        # Code département standardisé sur 2 caractères (01, 67, etc.)
        .withColumn(
            "departement_code",
            F.lpad(F.col("tv016_departement_code").cast("string"), 2, "0")
        )

        # Extraction du nom du département depuis "67 - Bas Rhin" -> "Bas Rhin"
        .withColumn(
            "departement_nom",
            F.trim(
                F.regexp_replace(
                    F.col("tv016_departement_departement"),
                    r"^[0-9A-Za-z]{2,3}\s*-\s*",
                    ""
                )
            )
        )

        # Suppression des colonnes brutes
        .drop("tv016_departement_code", "tv016_departement_departement")
    )
    return curated

def main():
    spark = SparkSession.builder.appName("curate-dpe-all-deps").getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    print(f"Lecture des fichiers RAW dans {RAW_DIR}")
    files = list_raw_files(spark)

    # Si aucun fichier RAW ne correspond au pattern attendu, on stoppe
    if not files:
        raise RuntimeError("Aucun fichier RAW trouvé (pattern dpe_<dep>_raw_*.jsonl.gz)")

    # On garde uniquement le fichier le plus récent par département
    latest_by_dep = {}
    for dep, path in files:
        latest_by_dep[dep] = path

    ok, ko = [], []

    for dep in sorted(latest_by_dep.keys()):
        raw_path = latest_by_dep[dep]
        curated_dir = f"hdfs:///{CURATED_BASE_DIR}/dpe_{dep.lower()}_curated"

        try:
            print("------------------------------------------------------------")
            print(f"Département {dep}")
            print(f"RAW    : {raw_path}")
            print(f"CURATED: {curated_dir}")

            # Lecture du fichier RAW (JSONL gzip) depuis HDFS
            df = spark.read.json(raw_path)

            # Application des transformations
            curated = transform(df)

            # Écriture en Parquet dans la zone Curated
            curated.write.mode("overwrite").parquet(curated_dir)

            print("Lecture de la table Curated et affichage de 10 lignes :")
            spark.read.parquet(curated_dir).show(10, truncate=False)

            ok.append(dep)

        except Exception as e:
            # En cas d’erreur sur un département, on continue avec les suivants
            print(f"Département {dep} : échec -> {e}")
            ko.append(dep)

    # Résumé
    print("------------------------------------------------------------")
    print(f"Succès ({len(ok)}) : " + (", ".join(ok) if ok else "aucun"))
    print(f"Échecs ({len(ko)}) : " + (", ".join(ko) if ko else "aucun"))

    spark.stop()

    if ko:
        raise SystemExit(2)

if __name__ == "__main__":
    main()