import { useEffect, useState } from "react";
import { api } from "../api/api";
import { useLocation } from "react-router-dom";

const PdfIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M6 2h9l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"
      stroke="currentColor"
      strokeWidth="2"
    />
    <path
      d="M14 2v6h6"
      stroke="currentColor"
      strokeWidth="2"
    />
    <text
      x="6.5"
      y="17"
      fontSize="7"
      fill="currentColor"
      fontWeight="bold"
    >
      PDF
    </text>
  </svg>
);

export default function ClientRentReceipt() {
  const [quittances, setQuittances] = useState([]);
  const [error, setError] = useState(false);
  const [loadingId, setLoadingId] = useState(null);
  const [successId, setSuccessId] = useState(null);
  const location = useLocation();

  const locataireId = localStorage.getItem("tenantId");

  const fetchQuittances = () => {
   api
    .get(`/rent-receipt/client/${locataireId}`)
    .then(res => setQuittances(res.data))
    .catch(() => setError(true));
};
  useEffect(() => {
    fetchQuittances();
    const interval = setInterval(() => {
      fetchQuittances();
    }, 2000);

    return () => clearInterval(interval); // nettoyage
  }, []);
  useEffect(() => {
      const params = new URLSearchParams(location.search);
      const receiptId = params.get("receiptId");
      if (!receiptId) return;

      const el = document.getElementById(`receipt-${receiptId}`);
      if (!el) return;

      el.scrollIntoView({ behavior: "smooth", block: "center" });

      el.classList.remove("receipt-highlight");
      void el.offsetHeight;
      el.classList.add("receipt-highlight");

      setTimeout(() => el.classList.remove("receipt-highlight"), 2000);
      window.history.replaceState({}, "", "/client");
  }, [location.search, quittances]);

  const formatPeriode = (periode) => {
    const [year, month] = periode.split("-");
    return new Date(year, month - 1).toLocaleDateString("fr-FR", {
      month: "long",
      year: "numeric"
    });
  };

  const downloadQuittance = (quittanceId) => {
    setLoadingId(quittanceId);

    api
      .post("/rent-receipt/download", null, {
        params: { quittanceId }
      })
      .then(res => {
        const link = document.createElement("a");
        link.href = res.data;
        link.download = "quittance.pdf";
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        setSuccessId(quittanceId);
        setTimeout(() => setSuccessId(null), 3000);
      })
      .finally(() => setLoadingId(null));
  };

  return (
    <div className="section">
      <h2 className="section-title">Mes quittances</h2>

      {error && (
        <div className="message error">
          Impossible de charger les quittances
        </div>
      )}

      {!error && quittances.length === 0 && (
        <div className="empty-state">
          Aucune quittance disponible pour le moment
        </div>
      )}

      {quittances.length > 0 && (
        <div className="receipts-wrapper">
          <div className="receipts-table">
            <div className="receipts-header">
              <span>Période</span>
              <span>Total</span>
              <span>Statut</span>
              <span>Action</span>
            </div>

            {quittances
              .sort((a, b) => b.periode.localeCompare(a.periode))
             .map(q => (
                 <div key={q.id} id={`receipt-${q.id}`} className="receipts-row">
                 <span>{formatPeriode(q.periode)}</span>

                 <span className="amount">
                   {q.montant} €
                 </span>

                 <span className="status paid">PAYÉ</span>

                 <button
                   className={`download-btn ${
                     loadingId === q.id ? "loading" : ""
                   }`}
                   onClick={() => downloadQuittance(q.id)}
                   disabled={loadingId === q.id}
                 >
                   <PdfIcon />
                   <span>
                     {loadingId === q.id
                       ? "Téléchargement…"
                       : "Télécharger"}
                   </span>
                 </button>

                 {successId === q.id && (
                   <span className="download-success">
                          Téléchargement réussi
                   </span>
                 )}

               </div>
             ))}
          </div>
        </div>
      )}
    </div>
  );
}
