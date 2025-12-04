import { useState, useEffect } from "react";
import "./App.css";

function App() {
    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [message, setMessage] = useState("");
    const [messageType, setMessageType] = useState("");
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(false);

    const API_URL = 'http://172.31.253.250:8081/api/auth/access';


    // Charger l'historique au démarrage
    useEffect(() => {
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        try {
            const res = await fetch(`${API_URL}/logs`);
            const data = await res.json();
            setHistory(data.reverse());  // plus récent en haut
        } catch (err) {
            console.error("Erreur historique :", err);
        }
    };

    const handleValidate = async (e) => {
        e.preventDefault();

        if (!firstName.trim() || !lastName.trim()) {
            setMessage("Veuillez saisir le nom et le prénom.");
            setMessageType("warning");
            return;
        }

        try {
            setLoading(true);
            setMessage("");

            const res = await fetch(
                `${API_URL}/validate?firstName=${encodeURIComponent(firstName)}&lastName=${encodeURIComponent(lastName)}`
            );

            const data = await res.json();

            if (data.accessGranted) {
                setMessage("Accès autorisé ✔️");
                setMessageType("success");
            } else {
                setMessage(`Accès refusé ❌ — ${data.reason}`);
                setMessageType("error");
            }

            setFirstName("");
            setLastName("");
            setTimeout(fetchHistory, 500); // rafraîchir l'historique
        } catch (err) {
            setMessage("Erreur : " + err.message);
            setMessageType("error");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">

            <h1>Contrôle d'accès JSA Home</h1>

            {/* Formulaire */}
            <form onSubmit={handleValidate} className="form">

                <label>Prénom</label>
                <input
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="Ex : Aya"
                    disabled={loading}
                />

                <label>Nom</label>
                <input
                    type="text"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="Ex : Elk"
                    disabled={loading}
                />

                <button type="submit" disabled={loading}>
                    {loading ? "Validation..." : "Valider"}
                </button>
            </form>

            {/* Message retour */}
            {message && (
                <div className={`message ${messageType}`}>
                    {message}
                </div>
            )}

            {/* Historique */}
            <h2>Historique des accès</h2>

            {history.length === 0 ? (
                <p>Aucun accès enregistré pour le moment.</p>
            ) : (
                <ul className="history">
                    {history.map((item) => (
                        <li
                            key={item.id}
                            className={item.accessGranted ? "ok" : "denied"}
                        >
                            <strong>{item.firstName} {item.lastName}</strong>
                            {" — "}
                            {item.accessGranted ? "Autorisé" : "Refusé"}
                            {" — "}
                            <em>{item.reason}</em>
                            <br />
                            <small>{new Date(item.accessTimestamp).toLocaleString()}</small>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

export default App;
