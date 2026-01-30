from pyspark.sql import SparkSession
from pyspark.sql import functions as F

def main():
    spark = SparkSession.builder.appName("curate-dpe-01").getOrCreate()
    spark.sparkContext.setLogLevel("WARN")

    # -----------------------------
    # Chemins HDFS
    # -----------------------------
    # Fichier RAW : JSONL compressé
    RAW_FILE = "hdfs:///data/raw/properties/dpe_01_raw_20260129_182151.jsonl.gz"

    # Dossier CURATED : Parquet
    CURATED_DIR = "hdfs:///data/curated/properties/dpe_01_curated"

    print("Lecture du fichier RAW")
    df = spark.read.json(RAW_FILE)

    # -----------------------------
    # Colonnes attendues dans le RAW
    # -----------------------------
    required = [
        "classe_consommation_energie",       # classe DPE (A..G)
        "tr001_modele_dpe_type_libelle",     # statut du bien (Vente/Location)
        "tv016_departement_code",            # code département (ex: 67, 2A...)
        "tv016_departement_departement"      # libellé avec code (ex: "67 - Bas Rhin")
    ]

    # Vérifie que toutes les colonnes existent avant de transformer
    missing = [c for c in required if c not in df.columns]
    if missing:
        raise RuntimeError(f"Colonnes manquantes dans le RAW : {missing}")

    print("Sélection + transformation des colonnes")
    curated = (
        # On ne garde que les colonnes utiles
        df.select(*required)

        # Renommage des colonnes
        .withColumnRenamed("classe_consommation_energie", "classe_dpe")
        .withColumnRenamed("tr001_modele_dpe_type_libelle", "statut_bien")

        # Normalisation de la classe DPE :
        #    - trim : supprime les espaces
        #    - upper : force en majuscules (ex: "c" -> "C")
        .withColumn("classe_dpe", F.upper(F.trim(F.col("classe_dpe"))))

        # Normalisation du statut du bien :
        #    - trim : supprime les espaces
        #    - initcap : met en forme "Titre" (ex: "location" -> "Location")
        .withColumn("statut_bien", F.initcap(F.trim(F.col("statut_bien"))))

        # Garder uniquement les biens en location
        .filter(F.col("statut_bien") == "Location")

        # Création d'un code département standardisé sur 2 caractères :
        #    - cast en string
        #    - lpad(2, "0") : ex 1 -> "01", 67 -> "67"
        .withColumn(
            "departement_code",
            F.lpad(F.col("tv016_departement_code").cast("string"), 2, "0")
        )

        # Extraction du nom du département :
        #    tv016_departement_departement contient "67 - Bas Rhin"
        #    - regexp_replace enlève le préfixe "67 - "
        #    - trim nettoie les espaces restants
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

    print("Écriture en zone Curated (Parquet)")
    curated.write.mode("overwrite").parquet(CURATED_DIR)

    print("Lecture de la table Curated et affichage de 10 lignes")
    spark.read.parquet(CURATED_DIR).show(10, truncate=False)

    print("Terminé.")
    print(f"Curated écrit dans : {CURATED_DIR}")

    spark.stop()

if __name__ == "__main__":
    main()