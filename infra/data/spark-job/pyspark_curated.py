from pyspark.sql import SparkSession
from pyspark.sql.functions import *


# ------------------------------------------------
# 1. Spark Session
# ------------------------------------------------
spark = SparkSession.builder \
    .appName("WebScraping_Curated_TwoTransformations") \
    .getOrCreate()


print(" Spark Session démarrée !")


# ------------------------------------------------
# 2. Lecture RAW
# ------------------------------------------------
raw_path = "hdfs:///data/raw/webscraping/books.csv"


df_raw = spark.read.csv(raw_path, header=True, inferSchema=True)


print(" Données RAW chargées :")
df_raw.show(5)


# ------------------------------------------------
# 3. Transformations (3 transformations)
# ------------------------------------------------


# Transformation 1 : titre en minuscule
df_t1 = df_raw.withColumn(
    "title",
    lower(trim(col("title")))
)


# Transformation 2 : nettoyer la colonne price (extraire numéro + cast en double)
df_t2 = df_t1.withColumn(
    "price",
    regexp_extract(col("price"), r"([0-9]+\.[0-9]+)", 1).cast("double")
)


# Transformation 3 : catégoriser les prix
df_curated = df_t2.withColumn(
    "price_category",
    when(col("price") < 20, "Cheap")
    .when((col("price") >= 20) & (col("price") <= 40), "Medium")
    .otherwise("Expensive")
)


print(" Données transformées :")
df_curated.show(10)


# ------------------------------------------------
# 4. Sauvegarde CURATED
# ------------------------------------------------
curated_path = "hdfs:///data/curated/books_cleaned"


df_curated.write.mode("overwrite").parquet(curated_path)


print(f" Données CURATED enregistrées dans : {curated_path}")


# ------------------------------------------------
# 5. Fin
# ------------------------------------------------
spark.stop()
print(" Pipeline CURATED terminé")
