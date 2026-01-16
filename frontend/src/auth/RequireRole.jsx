import { Navigate } from "react-router-dom";
import { getRole } from "./authStorage";

export default function RequireRole({ role, children }) {
    return getRole() === role ? children : <Navigate to="/login" replace />;
}