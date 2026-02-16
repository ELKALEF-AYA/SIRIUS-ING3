import { useEffect, useState } from "react";
import LogoutButton from "../auth/LogoutButton";
import { api } from "../api/api";
import "../App.css";

export default function AgentDashboard() {
    const [locataires, setLocataires] = useState([]);
    const [locataireId, setLocataireId] = useState("");
    const [periode, setPeriode] = useState("");
    const [charges, setCharges] = useState("");
    const [preview, setPreview] = useState(null);

    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);


    const [showSuccess, setShowSuccess] = useState(false);



    useEffect(() => {
        api.get("/locataires")
            .then(res => setLocataires(res.data))
            .catch(() => setError("Impossible de charger les locataires"));
    }, []);


    const handlePreview = async () => {
        setError(null);
        setPreview(null);
        setLoading(true);

        try {
            const res = await api.get(
                  `/rent-receipt/preview?locataireId=${locataireId}&periode=${periode}&charges=${charges}`
            );
            setPreview(res.data);
        } catch {
            setError("Erreur lors de la prévisualisation");
        } finally {
            setLoading(false);
        }
    };


    const handleGenerate = async () => {
        try {
            await api.post("/rent-receipt/generate", null, {
                params: {
                    locataireId,
                    periode,
                    charges
                }
            });

            setShowSuccess(true);
        } catch {
            setError("Erreur lors de la génération de la quittance");
        }
    };
    const formatPeriode = (periode) => {
        if (!periode) return "";

        const [year, month] = periode.split("-");
        const date = new Date(year, month - 1);

        return date.toLocaleDateString("fr-FR", {
            month: "long",
            year: "numeric"
        });
    };


    return (
        <div className="page">
                <div style={{ display: "flex", justifyContent: "flex-end" }}>
                    <LogoutButton />
                </div>
            {/* FORM */}
            <div className="section">
                <h2 className="section-title">Création et génération d’une quittance</h2>

                {error && <div className="message error">{error}</div>}

                <div className="form">
                    <div>
                        <label>Locataire:</label>
                        <select
                            value={locataireId}
                            onChange={(e) => setLocataireId(e.target.value)}
                        >
                            <option value="">— Sélectionner un locataire —</option>
                            {locataires.map(l => (
                                <option key={l.id} value={l.id}>
                                    {l.prenom} {l.nom}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label>Période:</label>
                        <input
                            type="month"
                            value={periode}
                            onChange={(e) => setPeriode(e.target.value)}
                        />
                    </div>
                    <div>
                        <label>Charges du mois (€) :</label>
                        <input
                            type="number"
                            value={charges}
                            onChange={(e) => setCharges(e.target.value)}
                            placeholder="Ex : 80"
                        />
                    </div>


                    <button
                        className="primary"
                        onClick={handlePreview}
                        disabled={!locataireId || !periode || loading}
                    >
                        <i className="fa-solid fa-eye"></i>
                        Prévisualiser
                    </button>



                </div>
            </div>

            {/* SUMMARY */}
            {preview && (
                <div className="section summary">
                    <h2 className="section-title">
                               Récapitulatif de la quittance
                    </h2>

                    <ul className="summary-grid">

                        <li><strong>Locataire :</strong> {preview.locataireNom}</li>
                        <li><strong>Adresse :</strong> {preview.logementAdresse}</li>
                        <li><strong>Loyer :</strong> {preview.loyer} €</li>
                        <li><strong>Charges :</strong> {preview.charges} €</li>
                        <li><strong>Total :</strong> {preview.total} €</li>
                        <li>
                            <strong>Statut :</strong>{" "}
                            <span
                                className={`status ${
                                    preview.statut === "PAYÉ" ? "paid" : "unpaid"
                                }`}
                            >
                                {preview.statut}
                            </span>
                        </li>

                    </ul>

                    <button
                        className="secondary"
                        onClick={handleGenerate}
                        disabled={preview.statut !== "PAYÉ" || loading}
                    >
                        <i className="fa-solid fa-file-pdf"></i>
                        Générer la quittance PDF
                    </button>


                    {preview.statut !== "PAYÉ" && (
                        <div className="message warning">
                            La quittance ne peut être générée que si le statut est PAYÉ
                        </div>
                    )}
                </div>
            )}

            {/* 🟢 POPUP SUCCESS */}
            {showSuccess && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-icon">✓</div>

                        <h3>Quittance générée</h3>

                       <p>
                           La quittance de <strong>{preview.locataireNom}</strong> pour le mois de{" "}
                           <strong>{formatPeriode(periode)}</strong> a été générée avec succès.
                       </p>



                        <button onClick={() => setShowSuccess(false)}>
                            <i className="fa-solid fa-check"></i>
                            Fermer
                        </button>

                    </div>
                </div>
            )}

        </div>
    );
}
