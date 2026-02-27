from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime

ENV_FILE = "/home/jsa/airflow/dags/dpe/dpe.env"

FULL_SCRIPT = "/home/jsa/scraping/dpe/scrape_dpe_full_4m.py"

SPARK_USER = "jsa"
SPARK_JOB = "/home/jsa/spark-jobs/transform_dpe_curated.py"

with DAG(
        dag_id="dpe-full",
        start_date=datetime(2026, 2, 1),
        schedule=None,
        catchup=False,
        tags=["dpe", "full"],
) as dag:

    scrape_full = BashOperator(
        task_id="scrape_full",
        bash_command=f"""
        set -euo pipefail
        source {ENV_FILE}
        python3 {FULL_SCRIPT}
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

    scrape_full >> transform_curated
