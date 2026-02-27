"""
TRANSFORMATION RAW -> CURATED (FULL + DELTA)
"""

import json
from datetime import datetime, timezone

from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    col, when, trim, length, to_date, substring,
    lit, row_number, greatest, coalesce
)
from pyspark.sql.window import Window

# =========================
# CONFIG
# =========================

RAW_DATA_DIR = "/data/raw/dpe/data"
CURATED_DIR = "/data/curated/dpe"
CURATED_STATE_JSON = f"{CURATED_DIR}/_STATE.json"
BI_DIR = "/data/curated/dpe/bi"
BI_TMP_DIR = f"{BI_DIR}/_tmp_export_csv"
BI_SINGLE_CSV = f"{BI_DIR}/dpe_curated.csv"

COLS = {
    "numero_dpe": "numero_dpe",
    "etiquette_dpe": "classe_dpe",
    "code_departement_ban": "code_departement",
    "code_postal_brut": "code_postal",
    "type_batiment": "type_bien",
    "periode_construction": "periode_construction",
    "qualite_isolation_enveloppe": "qualite_isolation",
    "date_reception_dpe": "date_reception_dpe",
    "date_derniere_modification_dpe": "date_derniere_modification_dpe",
}
FINAL_COL_ORDER = list(COLS.values())[:4] + ["departement_final"] + list(COLS.values())[4:]

# =========================
# HELPERS HDFS
# =========================

def utc_now_iso():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def fs_handle(spark):
    jvm = spark._jvm
    conf = spark._jsc.hadoopConfiguration()
    fs = jvm.org.apache.hadoop.fs.FileSystem.get(conf)
    Path = jvm.org.apache.hadoop.fs.Path
    return fs, Path

def hdfs_exists(spark, path):
    fs, Path = fs_handle(spark)
    return fs.exists(Path(path))

def hdfs_glob(spark, pattern):
    fs, Path = fs_handle(spark)
    statuses = fs.globStatus(Path(pattern))
    if statuses is None:
        return []
    return [st.getPath().toString() for st in statuses]

def hdfs_delete(spark, path, recursive=True):
    fs, Path = fs_handle(spark)
    p = Path(path)
    if fs.exists(p):
        fs.delete(p, recursive)

def hdfs_rename(spark, src, dst):
    fs, Path = fs_handle(spark)
    src_p, dst_p = Path(src), Path(dst)
    parent = dst_p.getParent()
    if parent is not None and not fs.exists(parent):
        fs.mkdirs(parent)
    if fs.exists(dst_p):
        fs.delete(dst_p, False)
    ok = fs.rename(src_p, dst_p)
    if not ok:
        raise RuntimeError(f"Rename HDFS échoué: {src} -> {dst}")

def hdfs_write_small_json(spark, path, obj):
    fs, Path = fs_handle(spark)
    p = Path(path)
    parent = p.getParent()
    if parent is not None and not fs.exists(parent):
        fs.mkdirs(parent)

    out = fs.create(p, True)
    try:
        data = (json.dumps(obj, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
        out.write(data)
    finally:
        out.close()

def hdfs_read_small_json(spark, path):
    fs, Path = fs_handle(spark)
    p = Path(path)
    if not fs.exists(p):
        return {}

    inp = fs.open(p)
    try:
        b = spark._jvm.org.apache.commons.io.IOUtils.toByteArray(inp)
        txt = bytes(b).decode("utf-8")
        return json.loads(txt)
    except Exception:
        return {}
    finally:
        inp.close()

def extract_run_date_from_filename(path):
    base = path.split("/")[-1]
    prefix = "dpe_delta_run-"
    suffix = ".jsonl.gz"
    if base.startswith(prefix) and base.endswith(suffix):
        return base[len(prefix):-len(suffix)]
    return None

# =========================
# TRANSFORMATIONS
# =========================

def fill_nr(c):
    return when(col(c).isNull() | (length(trim(col(c))) == 0), lit("Non renseigné")).otherwise(col(c))

def transform_common(df):
    raw_cols = list(COLS.keys())
    existing = [c for c in raw_cols if c in df.columns]
    df = df.select([col(c) for c in existing])

    if "date_reception_dpe" in df.columns:
        df = df.withColumn("date_reception_dpe", to_date(col("date_reception_dpe"), "yyyy-MM-dd"))
    if "date_derniere_modification_dpe" in df.columns:
        df = df.withColumn("date_derniere_modification_dpe", to_date(col("date_derniere_modification_dpe"), "yyyy-MM-dd"))

    for c in ["etiquette_dpe","code_departement_ban","code_postal_brut","type_batiment",
              "periode_construction","qualite_isolation_enveloppe","numero_dpe"]:
        if c in df.columns:
            df = df.withColumn(c, fill_nr(c))

    if "code_departement_ban" in df.columns and "code_postal_brut" in df.columns:
        dep_from_cp = when(
            (col("code_postal_brut") != lit("Non renseigné")) &
            (length(trim(col("code_postal_brut"))) >= 2),
            substring(col("code_postal_brut"), 1, 2)
        ).otherwise(lit("Non renseigné"))

        df = df.withColumn(
            "departement_final",
            when(col("code_departement_ban") != lit("Non renseigné"), col("code_departement_ban")).otherwise(dep_from_cp)
        )
    else:
        df = df.withColumn("departement_final", lit("Non renseigné"))

    for src, dst in COLS.items():
        if src in df.columns and src != dst:
            df = df.withColumnRenamed(src, dst)

    existing_final = [c for c in FINAL_COL_ORDER if c in df.columns]
    df = df.select([col(c) for c in existing_final])

    return df

def dedup_keep_latest(df):
    w = Window.partitionBy("numero_dpe").orderBy(col("date_derniere_modification_dpe").desc())

    return (
        df.withColumn("_rn", row_number().over(w))
        .filter(col("_rn") == 1)
        .drop("_rn")
    )
def export_single_csv_for_bi(spark, df):
    hdfs_delete(spark, BI_TMP_DIR, recursive=True)

    (
        df.coalesce(1)
        .write.mode("overwrite")
        .option("header", "true")
        .option("delimiter", ";")
        .csv(BI_TMP_DIR)
    )

    parts = hdfs_glob(spark, f"{BI_TMP_DIR}/part-*.csv")
    if not parts:
        raise RuntimeError("Aucun fichier part-*.csv trouvé dans l'export BI.")

    hdfs_rename(spark, parts[0], BI_SINGLE_CSV)

    hdfs_delete(spark, BI_TMP_DIR, recursive=True)

    print(f"[OK] Fichier CSV BI prêt : {BI_SINGLE_CSV}", flush=True)

# =========================
# MAIN
# =========================

def main():
    spark = (
        SparkSession.builder
        .appName("DPE RAW->CURATED (FULL+DELTA)")
        .getOrCreate()
    )
    spark.sparkContext.setLogLevel("WARN")

    curated_exists = hdfs_exists(spark, CURATED_STATE_JSON)

    # ======================
    # FULL
    # ======================
    if not curated_exists:
        print("[MODE] FULL -> création CURATED", flush=True)

        full_files = hdfs_glob(spark, f"{RAW_DATA_DIR}/dpe_full_run-*_part-*.jsonl.gz")
        if not full_files:
            raise RuntimeError("Aucun fichier FULL trouvé.")

        df = spark.read.json(full_files)
        df = transform_common(df)
        df = dedup_keep_latest(df)

        df.write.mode("overwrite").parquet(CURATED_DIR)

        df_curated_written = spark.read.parquet(CURATED_DIR)
        export_single_csv_for_bi(spark, df_curated_written)

        now = utc_now_iso()
        hdfs_write_small_json(spark, CURATED_STATE_JSON, {
            "curated_built_from": "full",
            "curated_built_at_utc": now,
            "last_delta_integrated_date": None,
            "last_transform_run_at_utc": now
        })

        print("[OK] CURATED écrit (FULL)", flush=True)

    # ======================
    # DELTA
    # ======================
    else:
        print("[MODE] DELTA -> mise à jour CURATED", flush=True)

        curated_state = hdfs_read_small_json(spark, CURATED_STATE_JSON)
        last_integrated = curated_state.get("last_delta_integrated_date")

        delta_files = hdfs_glob(spark, f"{RAW_DATA_DIR}/dpe_delta_run-*.jsonl.gz")
        if not delta_files:
            print("[INFO] Aucun delta trouvé.", flush=True)

            hdfs_write_small_json(spark, CURATED_STATE_JSON, {
                "curated_built_from": curated_state.get("curated_built_from", "full"),
                "curated_built_at_utc": curated_state.get("curated_built_at_utc"),
                "last_delta_integrated_date": curated_state.get("last_delta_integrated_date"),
                "last_transform_run_at_utc": utc_now_iso(),
                "deltas_integrated_count": curated_state.get("deltas_integrated_count") or 0
            })

            spark.stop()
            return

        new_deltas = []
        for f in delta_files:
            d = extract_run_date_from_filename(f)
            if not d:
                continue
            if (last_integrated is None) or (d > last_integrated):
                new_deltas.append((d, f))

        new_deltas.sort(key=lambda x: x[0])

        if not new_deltas:
            print("[INFO] Aucun nouveau delta à intégrer.", flush=True)

            hdfs_write_small_json(spark, CURATED_STATE_JSON, {
                "curated_built_from": curated_state.get("curated_built_from", "full"),
                "curated_built_at_utc": curated_state.get("curated_built_at_utc"),
                "last_delta_integrated_date": curated_state.get("last_delta_integrated_date"),
                "last_transform_run_at_utc": utc_now_iso(),
                "deltas_integrated_count": curated_state.get("deltas_integrated_count") or 0
            })

            spark.stop()
            return

        df_curated = spark.read.parquet(CURATED_DIR)
        delta_paths = [p for (_, p) in new_deltas]

        df_delta = spark.read.json(delta_paths)
        df_delta = transform_common(df_delta)

        df = df_curated.unionByName(df_delta, allowMissingColumns=True)
        df = dedup_keep_latest(df)

        df.write.mode("overwrite").parquet(CURATED_DIR)

        df_curated_written = spark.read.parquet(CURATED_DIR)
        export_single_csv_for_bi(spark, df_curated_written)

        newest_delta = new_deltas[-1][0]

        hdfs_write_small_json(spark, CURATED_STATE_JSON, {
            "curated_built_from": curated_state.get("curated_built_from", "full"),
            "curated_built_at_utc": curated_state.get("curated_built_at_utc"),
            "last_delta_integrated_date": newest_delta,
            "last_transform_run_at_utc": utc_now_iso(),
            "deltas_integrated_count": len(new_deltas)
        })

        print(f"[OK] CURATED mis à jour (delta intégré jusqu'à {newest_delta})", flush=True)

    # ======================
    # Affichage final
    # ======================
    print("\n===== Aperçu CURATED (10 plus récentes) =====", flush=True)

    df_out = spark.read.parquet(CURATED_DIR)

    df_out.orderBy(
        col("date_derniere_modification_dpe").desc(),
        col("numero_dpe").desc()
    ).show(10, truncate=False)

    spark.stop()

if __name__ == "__main__":
    main()
