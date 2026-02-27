from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime

ENV_FILE = "/home/jsa/airflow/dags/dpe/dpe.env"

DELTA_SCRIPT = "/home/jsa/scraping/dpe/scrape_dpe_delta_weekly.py"

SPARK_USER = "jsa"
SPARK_JOB = "/home/jsa/spark-jobs/transform_dpe_curated.py"

with DAG(
        dag_id="dpe-weekly",
        start_date=datetime(2026, 2, 20),
        schedule="0 2 * * 1",
        catchup=False,
        tags=["dpe", "delta", "weekly"],
) as dag:

    scrape_delta = BashOperator(
        task_id="scrape_delta_weekly",
        bash_command=f"""
        set -euo pipefail
        source {ENV_FILE}
        python3 {DELTA_SCRIPT}
        """,
    )

    transform_curated = BashOperator(
        task_id="transform_raw_to_curated",
        bash_command=f"""
        set -euo pipefail
        source {ENV_FILE}
        ssh -o StrictHostKeyChecking=no {SPARK_USER}@$SPARK_VM_IP \
          "set -euo pipefail; /opt/spark/bin/spark-submit {SPARK_JOB}"
        """,
    )

    scrape_delta >> transform_curated
