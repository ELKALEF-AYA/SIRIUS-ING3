import { useNavigate } from "react-router-dom";
import { clearAuth } from "./authStorage";

export default function LogoutButton() {
    const navigate = useNavigate();

    const logout = () => {
        clearAuth();
        navigate("/login");
    };

    return (
        <button
            type="button"
            onClick={logout}
            style={{
                padding: "10px 14px",
                borderRadius: 12,
                border: "1px solid #e5e7eb",
                background: "white",
                color: "#111827",
                fontWeight: 600,
                cursor: "pointer",
            }}
            onMouseEnter={(e) => (e.currentTarget.style.background = "#f9fafb")}
            onMouseLeave={(e) => (e.currentTarget.style.background = "white")}
        >
            Se déconnecter
        </button>
    );
}