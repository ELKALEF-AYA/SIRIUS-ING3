
import { useState } from "react";
import ChatInterface from "../chat/ChatInterface";
import LogoutButton from "../auth/LogoutButton";
import { getFullName } from "../auth/authStorage";
import ClientTopPanel from "../components/ClientTopPanel";
import ClientRentReceipt from "./ClientRentReceipt";
import NotificationBell from "../notifications/NotificationBell";


export default function ClientHome() {
    const userId = localStorage.getItem("userId") || "1";
    const email = localStorage.getItem("email") || "Client";
    const fullName = getFullName();

    const [tab, setTab] = useState("rent");

    return (
        <div style={{ padding: 16 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
                <div>
                    <h2 style={{ margin: 0 }}>Espace Client</h2>
                    <p style={{ margin: "6px 0 0" }}>Connecté : {fullName || email || "Client"}</p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                    <NotificationBell />
                    <LogoutButton />
                </div>
            </div>


            <ClientTopPanel active={tab} onChange={setTab} />


            <div style={{ marginTop: 16 }}>
                {tab === "rent" && <ClientRentReceipt />}


                {tab === "chat" && (
                    <ChatInterface
                        userType="CLIENT"
                        userId={parseInt(userId, 10)}
                        userName={email}
                    />
                )}
            </div>
        </div>
    );
}

