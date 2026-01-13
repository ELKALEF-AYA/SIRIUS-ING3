import LogoutButton from "../auth/LogoutButton";

export default function AgentHome() {
    const email = localStorage.getItem("email");
    const role = localStorage.getItem("role");

    return (
        <div style={{ padding: 16 }}>
            <h2>Agent Dashboard</h2>
            <p>Connecté : {email} ({role})</p>
            <LogoutButton />
        </div>
    );
}