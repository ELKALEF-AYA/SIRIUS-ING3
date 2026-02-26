from pyspark.sql import SparkSession
from pyspark.sql.functions import col, lit, when, monotonically_increasing_id, regexp_replace

# ============================================================
# 1 Spark Session
# ============================================================
spark = SparkSession.builder \
    .appName("Pipeline Score Payment Curated") \
    .getOrCreate()

spark.sparkContext.setLogLevel("ERROR")

print("DEMARRAGE DU PIPELINE SCORE PAYMENT")

# ============================================================
# 2 PATHS HDFS
# ============================================================
RAW_PATH = "/data/raw/Score_Payment"
CURATED_PATH = "/data/curated/Score_Payment_curated"

# ============================================================
# 3 LECTURE & EXTRACTION DES SCORES
# ============================================================

def read_score(file_pattern, column_name):
    return spark.read \
        .option("header", True) \
        .option("sep", ";") \
        .csv(f"{RAW_PATH}/{file_pattern}") \
        .select(
        regexp_replace(col(column_name), ",", ".")
        .cast("double")
        .alias("score")
    )

print("Lecture des sources...")

df_reg       = read_score("elections_regionales_2021_2nd_tour*.csv.gz", "exp_vot")
df_fdep      = read_score("indice_defavorisation_sociale_fdep*.csv.gz", "t1_txchom0")
df_fisc      = read_score("fiscalite_locale_communes*.csv.gz", "building_property_tax")
df_muni      = read_score("elections_municipales_2020_1er_tour*.csv.gz", "exp_vot")
df_euro      = read_score("elections_europeennes_2019*.csv.gz", "voix_exp")
df_colleges  = read_score("indicateurs_valeur_ajoutee_colleges*.csv.gz", "taux_d_acces_6eme_3eme")
df_env       = read_score("scores_multiexposition_environnement_idf*.csv.gz", "propbruit")

# Nouvelle source carburants
df_carburant = read_score("carburants/prix_carburants_*.csv.gz", "reg_code")
# ============================================================
# 4 CONSOLIDATION
# ============================================================

print("Consolidation des scores...")

df_scores = df_reg \
    .unionByName(df_fdep) \
    .unionByName(df_fisc) \
    .unionByName(df_muni) \
    .unionByName(df_euro) \
    .unionByName(df_colleges) \
    .unionByName(df_env) \
    .unionByName(df_carburant)

# ============================================================
# 5 NETTOYAGE + CHAMPS METIER
# ============================================================

print("Nettoyage et enrichissement...")

df_scores = df_scores.filter(col("score").isNotNull())

df_scores = df_scores \
    .withColumn("annee", lit(2025)) \
    .withColumn(
    "statut",
    when(col("score") <= 20, "BON PAYEUR")
    .when(col("score") <= 50, "MOYEN")
    .otherwise("MAUVAIS PAYEUR")
) \
    .withColumn("score_id", monotonically_increasing_id() + 1)

df_scores = df_scores.select(
    "score_id",
    "score",
    "statut",
    "annee"
)

print("Nombre total de scores valides :", df_scores.count())

# ============================================================
# 6 ECRITURE CSV UNIQUE EN ZONE CURATED
# ============================================================

print("Ecriture en zone CURATED...")

df_scores \
    .coalesce(1) \
    .write \
    .mode("overwrite") \
    .option("header", "true") \
    .option("delimiter", ",") \
    .csv(CURATED_PATH)

# ============================================================
# 7 RENOMMAGE DU FICHIER CSV
# ============================================================

print("Renommage du fichier genere...")

fs = spark._jvm.org.apache.hadoop.fs.FileSystem.get(
    spark._jsc.hadoopConfiguration()
)

path = spark._jvm.org.apache.hadoop.fs.Path(CURATED_PATH)
files = fs.listStatus(path)

part_file = None

for file in files:
    name = file.getPath().getName()
    if name.startswith("part-") and name.endswith(".csv"):
        part_file = file.getPath()

if part_file:
    new_file = spark._jvm.org.apache.hadoop.fs.Path(
        CURATED_PATH + "/score_payment_curated.csv"
    )

    if fs.exists(new_file):
        fs.delete(new_file, True)

    fs.rename(part_file, new_file)

success_file = spark._jvm.org.apache.hadoop.fs.Path(
    CURATED_PATH + "/_SUCCESS"
)

if fs.exists(success_file):
    fs.delete(success_file, True)

print("Fichier score_payment_curated.csv pret dans HDFS")

# ============================================================
# 8 ARRET PROPRE
# ============================================================

spark.stop()

print("SparkSession arretee - Pipeline termine avec succes")