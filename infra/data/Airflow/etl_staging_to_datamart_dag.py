from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime

default_args = {"owner": "jsa", "retries": 1}

with DAG(
        dag_id="etl_staging_to_datamart_dag",
        default_args=default_args,
        start_date=datetime(2026, 1, 1),
        schedule_interval=None,
        catchup=False,
        tags=["datamart"],
) as dag:
    etl_task = BashOperator(
        task_id="etl_staging_to_datamart",
        bash_command=(
            "ssh jsa@172.31.253.167 "
            "'/opt/spark/bin/spark-submit "
            "--conf spark.ui.enabled=false "
            "--conf spark.eventLog.enabled=false "
            "--jars /home/jsa/apps/nifi-1.28.1/lib/postgresql-42.7.3.jar "
            "/home/jsa/spark-jobs/pyspark_pipeline/etl_staging_to_datamart.py'"
        )
    )