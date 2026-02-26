from py4j.java_gateway import java_import
from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, lit, least, when, floor, rand, monotonically_increasing_id,
    year, current_date, lower, concat
)

spark = SparkSession.builder \
    .appName("Pipeline Tenants Curated") \
    .getOrCreate()

spark.sparkContext.setLogLevel("ERROR")

print("=" * 80)
print("DÉMARRAGE DU PIPELINE DE TRANSFORMATION - ZONE CURATED (TENANTS)")
print("=" * 80)

RAW_PATH = "/data/raw/tenants-jsa"
def get_latest_file(folder_name):
    fs = spark._jvm.org.apache.hadoop.fs.FileSystem.get(
        spark._jsc.hadoopConfiguration()
    )

    path = spark._jvm.org.apache.hadoop.fs.Path(f"{RAW_PATH}/{folder_name}")
    files = fs.listStatus(path)

    latest_file = None
    latest_time = 0

    for file in files:
        if file.isFile():
            mod_time = file.getModificationTime()
            if mod_time > latest_time:
                latest_time = mod_time
                latest_file = file.getPath().toString()

    return latest_file


CURATED_PATH = "/data/curated/tenants_curated"

PATH_DECES = f"{RAW_PATH}/deces"
PATH_MEDECINS = get_latest_file("medecins")
PATH_ANNUAIRE = get_latest_file("annuaire_pro")

PATH_EURO = get_latest_file("elections_2019")
PATH_PRES = get_latest_file("presidentielle_2022")
PATH_LEG = get_latest_file("legislatives_2022")

# ============================================================
# BLOC 1 — LOCATAIRES (DECES)
# ============================================================

print("\n[1] TRAITEMENT DES LOCATAIRES — DONNÉES PERSONNELLES")
print("Lecture des données brutes des locataires depuis la zone RAW...")

df_locataires = spark.read.option("header", True).option("sep", ";").csv(PATH_DECES)

print("Données locataires chargées avec succès !")

print("Sélection et renommage des colonnes utiles...")
df_locataires = df_locataires.select(
    col("lastname").alias("nom"),
    col("firstnames").alias("prenom"),
    col("sex").alias("sexe"),
    col("birth_date").alias("date_naissance"),
    col("current_birth_dep_name").alias("departement"),
    col("current_birth_reg_name").alias("region")
)

print("Calcul et normalisation de l’âge")
# Calcul de l’âge brut
df_locataires = df_locataires.withColumn(
    "age_brut",
    year(current_date()) - year(col("date_naissance"))
)

# Normalisation métier de l’âge
df_locataires = df_locataires.withColumn(
    "age",
    when(col("age_brut") < 18, None)
    .when(col("age_brut") <= 63, col("age_brut"))
    .otherwise(floor(rand() * (60 - 40 + 1)) + 40)
).drop("age_brut", "date_naissance")


df_locataires = df_locataires.withColumn(
    "locataire_id",
    monotonically_increasing_id() + 1
)

print(" Transformation des données locataires terminée")

# ============================================================
# BLOC 2 — CONTEXTE PROFESSIONNEL (SANTE)
# ============================================================
print("\n[2] CONTEXTE PROFESSIONNEL – Enrichissement des profils locataires")
print("Intégration des données santé pour enrichir les profils locataires...")

df_med = spark.read.option("header", True).option("sep", ";").csv(PATH_MEDECINS)
df_ann = spark.read.option("header", True).option("sep", ";").csv(PATH_ANNUAIRE)

print("Données professionnelles chargées avec succès !")


df_med = df_med.withColumnRenamed("column_13", "nature_exercice")
df_med = df_med.withColumnRenamed("column_14", "convention")

# Harmonisation colonnes
df_med = df_med.select(
    lower(col("column_12")).alias("profession"),
    col("nature_exercice"),
    col("convention"))

df_ann = df_ann.select(
    lower(col("libelle_profession")).alias("profession"),
    col("nature_exercice"),
    col("convention")
)

df_med = df_med.filter(~col("nature_exercice").rlike("^[0-9]+(\\.[0-9]+)?$"))
df_ann = df_ann.filter(~col("nature_exercice").rlike("^[0-9]+(\\.[0-9]+)?$"))

df_sante = df_med.unionByName(df_ann, allowMissingColumns=True)

print("Construction du champ : type d'emploi")

df_sante = df_sante.withColumn(
    "type_emploi",
    when(col("nature_exercice") == "Libéral intégral", "CDI / Emploi stable")
    .when(col("nature_exercice") == "Libéral activité salariée", "Freelance / Indépendant")
    .when(col("nature_exercice") == "Libéral temps partiel hospitalier", "Temps partiel / Contrat hybride")
    .when(col("nature_exercice") == "Libéral temps plein hospitalier", "CDI secteur public")
    .when(col("nature_exercice") == "N'exerce pas actuellement", "Sans emploi / Situation précaire")
    .when(col("nature_exercice") == "T. plein hosp. contrat mixte", "CDD long / Contrat spécifique")
    .when(col("nature_exercice") == "T. plein hosp./mal. aut. med.", "Inactif temporaire")
    .when(col("nature_exercice") == "Pharmacie mutualiste", "Structure mutualiste")
    .otherwise("Autre")
)

# ---- Catégorie professionnelle + revenu
print("Construction des champs : categorie professionnelle et niveau de revenu")

df_sante = df_sante.withColumn(
    "categorie_professionnelle",


       when(col("profession").isNull(),
         when(rand() < 0.1, "Cadre dirigeant")
         .when(rand() < 0.3, "Cadre supérieur")
         .when(rand() < 0.6, "Profession intermédiaire")
         .otherwise("Employé")
    )

    # -------------------------
    # Chirurgiens (très haut revenu)
    # -------------------------
    .when(col("profession").rlike("chirurgien"),
         "Cadre dirigeant")

    # -------------------------
    # Spécialistes médicaux
    # -------------------------
    .when(col("profession").rlike(
        "cardiologue|dermatologue|neurologue|radiologue|oncologue|"
        "endocrinologue|néphrologue|pneumologue|psychiatre|pédiatre|"
        "gynécologue|anesthésiste|obstétricien|gastro|hématologue"
    ),
        "Cadre supérieur")

    # -------------------------
    # Paramédical
    # -------------------------
    .when(col("profession").rlike(
        "sage-femme|kinésithérapeute|orthophoniste|infirmier"
    ),
        "Profession intermédiaire")

    .otherwise("Employé")
)
print("Construction du champ métier : intitule_poste")
print("Logique : attribution des postes selon la catégorie professionnelle")

df_sante = df_sante.withColumn(
    "intitule_poste",

    # ==================================================
    # CADRE DIRIGEANT
    # ==================================================
    when(col("categorie_professionnelle") == "Cadre dirigeant",
         when(rand() < 0.33, "Président")
         .when(rand() < 0.66, "Directeur général")
         .otherwise("CEO")
    )

    # ==================================================
    # CADRE SUPÉRIEUR
    # ==================================================
    .when(col("categorie_professionnelle") == "Cadre supérieur",
          when(rand() < 0.33, "Ingénieur senior")
          .when(rand() < 0.66, "Chef de projet")
          .otherwise("Data Scientist"
    )

    # ==================================================
    # PROFESSION INTERMÉDIAIRE
    # ==================================================
    .when(col("categorie_professionnelle") == "Profession intermédiaire",
          when(rand() < 0.33, "Technicien")
          .when(rand() < 0.66, "Comptable")
          .otherwise("Chargé de clientèle")
    )

    # ==================================================
    # EMPLOYÉ
    # ==================================================
    .otherwise(
        when(rand() < 0.33, "Employé administratif")
        .when(rand() < 0.66, "Vendeur")
        .otherwise("Agent logistique")
    )
)

df_sante = df_sante.withColumn(
    "niveau_revenu",
    when(col("categorie_professionnelle").isin("Cadre dirigeant", "Cadre supérieur"), "Élevé")
    .when(col("categorie_professionnelle") == "Profession intermédiaire", "Moyen")
    .otherwise("Faible")
)
print("Détermination du niveau d’urgence du locataire...")
print("Règle : classification selon le secteur de convention (Secteur 1 / Secteur 2)")

#  Niveau d’urgence
motif_faible = when(rand() < 0.33, "bail en cours") \
    .when(rand() < 0.66, "logement stable") \
    .otherwise("aucune contrainte")

motif_moyenne = when(rand() < 0.33, "fin de bail proche") \
    .when(rand() < 0.66, "logement temporaire") \
    .otherwise("mutation professionnelle")

motif_elevee = when(rand() < 0.33, "fin de bail imminente") \
    .when(rand() < 0.66, "préavis donné") \
    .otherwise("risque d’expulsion")

df_sante = df_sante.withColumn(
    "niveau_urgence",
    when(col("convention").rlike("Secteur 1") & ~col("convention").rlike("dépassement"),
         "Urgence faible")
    .when(col("convention").rlike("Secteur 1") & col("convention").rlike("dépassement"),
         "Urgence moyenne")
    .when(col("convention").rlike("Secteur 2"),
         "Urgence élevée")
    .otherwise("Urgence moyenne")
)

df_sante = df_sante.withColumn(
    "motif_urgence",
    when(col("niveau_urgence") == "Urgence faible", motif_faible)
    .when(col("niveau_urgence") == "Urgence moyenne", motif_moyenne)
    .when(col("niveau_urgence") == "Urgence élevée", motif_elevee)
)

df_sante = df_sante.drop("profession", "nature_exercice", "convention")

print("Génération de l’identifiant contexte_pro_id")
df_sante = df_sante.withColumn(
    "contexte_pro_id",
    monotonically_increasing_id() + 1
)

print("Enrichissement du contexte professionnel terminé")

# ============================================================
# BLOC 3 — CONTEXTE FINANCIER (ELECTIONS)
# ============================================================
print("\n[3] ÉVALUATION DE LA FIABILITÉ FINANCIÈRE DES LOCATAIRES")
print(" Calcul du score de fiabilité basé sur les données électorales...")

# --- 1) Lecture des 3 datasets élections
df_euro = spark.read.option("header", True).option("sep", ";").csv(PATH_EURO)
df_pres = spark.read.option("header", True).option("sep", ";").csv(PATH_PRES)
df_leg = spark.read.option("header", True).option("sep", ";").csv(PATH_LEG)

print("Sources financières chargées !")
# --- 2) Sélection et harmonisation de la colonne % voix / inscrits

# Européennes 2019
df_euro = df_euro.select(
    col("voix_ins").cast("double").alias("taux_voix_inscrits")
)

# Présidentielle 2022
df_pres = df_pres.select(
    col("voix_ins").cast("double").alias("taux_voix_inscrits")
)

# Législatives 2022
df_leg = df_leg.select(
    col("voix_ins").cast("double").alias("taux_voix_inscrits")
)

# --- 3) UNION des 3 datasets
df_elections = df_euro.unionByName(df_pres).unionByName(df_leg)

print("Ajustement du score de fiabilité financière basé sur le taux de voix des inscrits")
print("   Règle : score = min(taux_voix_inscrits × 100, 100)")
# --- 4) Calcul du score de fiabilité
# Règle : score = min(taux_voix_inscrits * 100, 100)
df_elections = df_elections.withColumn(
    "score_temp",
    least(col("taux_voix_inscrits") * 100, lit(100))
)

df_elections = df_elections.withColumn(
    "score_fiabilite",
    when(col("score_temp") == 0,
         floor(rand() * (80 - 30 + 1)) + 30
    ).otherwise(col("score_temp"))
).drop("score_temp")
# --- 5) Création de l’identifiant de contexte financier
df_elections = df_elections.withColumn(
    "contexte_financier_id",
    monotonically_increasing_id() + 1
)

# --- 6) Table finale du bloc 3
df_elections = df_elections.select(
    "contexte_financier_id",
    "score_fiabilite"
)
print("Bloc SCORE DE FIABILITÉ FINANCIÈRE finalisé")
# ============================================================
# BLOC 4 — ENRICHISSEMENT FINAL
# ============================================================
df_final = df_locataires \
    .withColumn("contexte_pro_id", (col("locataire_id") % df_sante.count()) + 1) \
    .withColumn("contexte_financier_id", (col("locataire_id") % df_elections.count()) + 1)

df_final = df_final.join(df_sante, "contexte_pro_id", "left") \
                   .join(df_elections, "contexte_financier_id", "left")

print("\n[4] CONSTRUCTION DE LA TABLE FINALE - TENANTS CURATED")
print(" Nombre total de lignes générées :", df_final.count())

print("\nAperçu des 5 premières lignes de la table finale :")
df_final.select(
    "nom",
    "prenom",
    "age",
    "departement",
    "region",
    "type_emploi",
    "categorie_professionnelle",
    "intitule_poste",
    "niveau_revenu",
    "niveau_urgence",
    "motif_urgence",
    "score_fiabilite"
).show(5, truncate=False)
# ============================================================
# ÉCRITURE FINALE — UN SEUL FICHIER
# ============================================================
print("\n Export de la table finale vers la zone CURATED (HDFS)...")

df_final \
    .coalesce(1) \
    .write \
    .mode("overwrite") \
    .option("header", "true") \
    .option("delimiter", ",") \
    .csv(CURATED_PATH)

print("Arrêt du SparkSession - Pipeline terminé avec succès")

spark.stop()


print("Renommage du fichier généré...")

# Accès au FileSystem Hadoop
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
        CURATED_PATH + "/locataires_curated.csv"
    )

    # Supprimer ancien fichier si existe
    if fs.exists(new_file):
        fs.delete(new_file, True)

    # Renommer
    fs.rename(part_file, new_file)

# Supprimer _SUCCESS
success_file = spark._jvm.org.apache.hadoop.fs.Path(
    CURATED_PATH + "/_SUCCESS"
)

if fs.exists(success_file):
    fs.delete(success_file, True)

print("Fichier renommé avec succès : locataires_curated.csv")
print("=" * 80)
print("PIPELINE TERMINÉ AVEC SUCCÈS")
print("=" * 80)