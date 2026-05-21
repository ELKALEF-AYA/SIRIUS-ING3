import requests
import time
import sys
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ============================================================
# CONFIG NIFI
# ============================================================
NIFI_BASE_URL = "https://172.31.253.167:8443/nifi-api"
NIFI_USERNAME = "dfcedbba-3dd5-4793-b6fc-e7ead54fc9bf"
NIFI_PASSWORD = "ydGoesqGYFWaJhgvbq7YPe8RSX1W5LoD"

PROCESS_GROUPS = {
    "payment": "019e1003-3093-1d62-d323-935886ee36c9",
    "tenant":  "46ff018f-019e-1000-ae26-bebc887010c3"
}

INITIAL_WAIT  = 30   # attente initiale avant le premier poll
POLL_INTERVAL = 30   # secondes entre chaque vérification
MAX_WAIT      = 600  # timeout max en secondes (10 min)


def get_token():
    """Obtenir un token JWT NiFi."""
    resp = requests.post(
        f"{NIFI_BASE_URL}/access/token",
        data={"username": NIFI_USERNAME, "password": NIFI_PASSWORD},
        verify=False
    )
    if resp.status_code != 201:
        raise Exception(f"Authentification NiFi échouée : {resp.status_code} {resp.text}")
    print("[INFO] Authentification NiFi réussie")
    return resp.text.strip()


def set_process_group_state(token, pg_id, state):
    """Démarrer (RUNNING) ou arrêter (STOPPED) un Process Group."""
    resp = requests.put(
        f"{NIFI_BASE_URL}/flow/process-groups/{pg_id}",
        json={"id": pg_id, "state": state},
        headers={"Authorization": f"Bearer {token}"},
        verify=False
    )
    if resp.status_code not in (200, 202):
        raise Exception(f"Impossible de passer le Process Group en {state} : {resp.status_code} {resp.text}")
    print(f"[INFO] Process Group {pg_id} → {state}")


def get_active_tasks(token, pg_id):
    """Retourner le nombre de tâches actives dans le Process Group."""
    resp = requests.get(
        f"{NIFI_BASE_URL}/process-groups/{pg_id}",
        headers={"Authorization": f"Bearer {token}"},
        verify=False
    )
    if resp.status_code != 200:
        raise Exception(f"Impossible de récupérer le statut : {resp.status_code} {resp.text}")
    status = resp.json().get("status", {}).get("aggregateSnapshot", {})
    return status.get("activeThreadCount", 0)


def wait_for_completion(token, pg_id):
    """Attendre que tous les threads soient terminés."""
    print(f"[INFO] Attente initiale de {INITIAL_WAIT}s pour laisser NiFi démarrer...")
    time.sleep(INITIAL_WAIT)

    elapsed = INITIAL_WAIT
    print(f"[INFO] Début du polling...")
    while elapsed < MAX_WAIT:
        active = get_active_tasks(token, pg_id)
        print(f"[DEBUG] Threads actifs : {active} (attente {elapsed}s/{MAX_WAIT}s)")
        if active == 0:
            print(f"[INFO] Traitement NiFi terminé après {elapsed}s")
            return
        time.sleep(POLL_INTERVAL)
        elapsed += POLL_INTERVAL

    raise Exception(f"Timeout NiFi dépassé ({MAX_WAIT}s) — flow toujours actif")


def trigger_nifi_flow(flow_name):
    """
    Déclencher un flow NiFi complet :
    1. Authentification
    2. Démarrage du Process Group
    3. Attente initiale + polling jusqu'à fin
    4. Arrêt du Process Group
    """
    if flow_name not in PROCESS_GROUPS:
        raise ValueError(f"Flow inconnu : {flow_name}. Valeurs valides : {list(PROCESS_GROUPS.keys())}")

    pg_id = PROCESS_GROUPS[flow_name]
    print(f"[INFO] Démarrage du flow NiFi : {flow_name} (ID: {pg_id})")

    token = get_token()
    set_process_group_state(token, pg_id, "RUNNING")
    wait_for_completion(token, pg_id)
    set_process_group_state(token, pg_id, "STOPPED")

    print(f"[INFO] Flow NiFi '{flow_name}' terminé avec succès")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage : python3 nifi_trigger.py <payment|tenant>")
        sys.exit(1)
    trigger_nifi_flow(sys.argv[1])