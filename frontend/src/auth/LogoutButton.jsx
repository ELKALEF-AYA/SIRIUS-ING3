import { useNavigate } from "react-router-dom";
import { clearAuth } from "./authStorage";

export default function LogoutButton() {
    const navigate = useNavigate();

    const logout = () => {
        clearAuth();
        navigate("/login");
    };

    return (
        <button onClick={logout} style={{ padding: 8 }}>
            Se déconnecter
        </button>
    );
}