import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/api";
import { saveAuth } from "../auth/authStorage";

export default function Login() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [err, setErr] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const onSubmit = async (e) => {
        e.preventDefault();
        setErr("");
        setLoading(true);

        try {
            const res = await api.post("/auth/login", { email, password });
            saveAuth(res.data);

            if (res.data.role === "AGENT") navigate("/agent");
            else navigate("/client");
        } catch (err) {
            const status = err?.response?.status;
            if (status === 401) setErr("Email ou mot de passe incorrect.");
            else setErr("Erreur technique.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: 360, margin: "80px auto", padding: 16 }}>
            <h2>Connexion</h2>

            <form onSubmit={onSubmit}>
                <div style={{ marginBottom: 12 }}>
                    <label>Email</label>
                    <input
                        style={{ width: "100%", padding: 10 }}
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                    />
                </div>

                <div style={{ marginBottom: 12 }}>
                    <label>Mot de passe</label>
                    <input
                        style={{ width: "100%", padding: 10 }}
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                    />
                </div>

                {err && <p style={{ color: "crimson" }}>{err}</p>}

                <button disabled={loading} style={{ width: "100%", padding: 10 }}>
                    {loading ? "Connexion..." : "Se connecter"}
                </button>
            </form>
        </div>
    );
}