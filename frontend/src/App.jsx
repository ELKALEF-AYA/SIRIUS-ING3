import { useEffect, useState } from "react";

function App() {
    const [message, setMessage] = useState("Chargement...");
    const [error, setError] = useState(null);

    useEffect(() => {
        // Appel du backend via Traefik
        fetch("/app/api/hello")
            .then((response) => {
                if (!response.ok) {
                    throw new Error("HTTP " + response.status);
                }
                return response.text();
            })
            .then((text) => setMessage(text))
            .catch((err) => {
                console.error(err);
                setError("Erreur lors de l'appel à l'API");
            });
    }, []);

    return (
        <div style={{ padding: "2rem", fontFamily: "sans-serif" }}>
            <h1>Frontend React</h1>
            <p><strong>Message du backend :</strong></p>
            {error ? (
                <p style={{ color: "red" }}>{error}</p>
            ) : (
                <p style={{ color: "green" }}>{message}</p>
            )}
        </div>
    );
}

export default App;