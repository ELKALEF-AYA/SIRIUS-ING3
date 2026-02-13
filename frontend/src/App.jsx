import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./pages/Login";
import AgentHome from "./pages/AgentHome";
import ClientHome from "./pages/ClientHome";
import RequireAuth from "./auth/RequireAuth";
import RequireRole from "./auth/RequireRole";
import AgentRentReceipt from "./pages/AgentRentReceipt";
import ClientRentReceipt from "./pages/ClientRentReceipt";



export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<Login />} />

                <Route
                    path="/agent"
                    element={
                        <RequireAuth>
                            <RequireRole role="AGENT">
                                <AgentRentReceipt />
                            </RequireRole>
                        </RequireAuth>
                    }
                />

                <Route
                    path="/client"
                    element={
                        <RequireAuth>
                            <RequireRole role="CLIENT">
                                <ClientRentReceipt />
                            </RequireRole>
                        </RequireAuth>
                    }
                />

                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="*" element={<Navigate to="/login" replace />} />
            </Routes>
        </BrowserRouter>
    );
}