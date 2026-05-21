from pyspark.sql import SparkSession
from pyspark.sql.functions import col, when, lit, row_number
from pyspark.sql.window import Window
from pyspark.sql import Row

spark = SparkSession.builder \
    .appName("ETL Staging to DataMart") \
    .getOrCreate()

spark.sparkContext.setLogLevel("ERROR")

DB_URL = "jdbc:postgresql://172.31.250.180:5432/jsa_db"
DB_PROPS = {"user": "jsa", "password": "aya", "driver": "org.postgresql.Driver"}

print("Lecture Staging...")
df_loc = spark.read.jdbc(DB_URL, "locataires_staging", properties=DB_PROPS)
df_scores = spark.read.jdbc(DB_URL, "scores_staging", properties=DB_PROPS)

# ============================================================
# DIM_LOCATAIRE
# ============================================================
print("Construction dim_locataire...")

df_dim_loc = df_loc.select("nom", "prenom", "age", "type_emploi", "niveau_revenu") \
    .distinct() \
    .withColumn("tranche_age",
                when(col("age") < 25, "moins de 25")
                .when(col("age") <= 35, "25-35")
                .when(col("age") <= 50, "35-50")
                .when(col("age") <= 65, "50-65")
                .otherwise("plus de 65")
                ) \
    .withColumn("locataire_id", row_number().over(Window.orderBy("nom", "prenom"))) \
    .select("locataire_id", "nom", "prenom", "age", "tranche_age", "type_emploi", "niveau_revenu")

df_dim_loc.write.jdbc(DB_URL, "dim_locataire", mode="append", properties=DB_PROPS)
nb_loc = df_dim_loc.count()
print(f"dim_locataire : {nb_loc} lignes")

# ============================================================
# DIM_REGION
# ============================================================
print("Construction dim_region...")

df_dim_region = df_loc.select("region", "departement") \
    .distinct() \
    .filter(col("region").isNotNull()) \
    .withColumn("region_id", row_number().over(Window.orderBy("region"))) \
    .select("region_id", "region", "departement")

df_dim_region.write.jdbc(DB_URL, "dim_region", mode="append", properties=DB_PROPS)
nb_reg = df_dim_region.count()
print(f"dim_region : {nb_reg} lignes")

# ============================================================
# DIM_DATE
# ============================================================
print("Construction dim_date...")

dim_date = spark.createDataFrame([Row(date_id=1, annee=2025)])
dim_date.write.jdbc(DB_URL, "dim_date", mode="append", properties=DB_PROPS)

# ============================================================
# DIM_CATEGORIE_RISQUE
# ============================================================
print("Construction dim_categorie_risque...")

categories = spark.createDataFrame([
    (1, "BON PAYEUR",     "0-24 jours/an",   "Fiable"),
    (2, "MOYEN",          "25-60 jours/an",  "Instable"),
    (3, "MAUVAIS PAYEUR", "61-365 jours/an", "Non fiable")
], ["categorie_id", "statut", "retard_annuel_moyen", "profil"])

categories.write.jdbc(DB_URL, "dim_categorie_risque", mode="append", properties=DB_PROPS)

# ============================================================
# FAIT_SCORE
# ============================================================
print("Construction fait_score...")

df_fait = df_scores \
    .withColumn("locataire_id", (col("score_id") % nb_loc) + 1) \
    .withColumn("region_id", (col("score_id") % nb_reg) + 1) \
    .withColumn("date_id", lit(1)) \
    .withColumn("categorie_id",
                when(col("score") <= 20, 1)
                .when(col("score") <= 50, 2)
                .otherwise(3)
                ) \
    .select(
    col("score_id"),
    col("locataire_id"),
    col("region_id"),
    col("date_id"),
    col("categorie_id"),
    col("score"),
    col("statut")
)

df_fait.write.jdbc(DB_URL, "fait_score", mode="append", properties=DB_PROPS)
print(f"fait_score : {df_fait.count()} lignes")

print("ETL termine !")
spark.stop()