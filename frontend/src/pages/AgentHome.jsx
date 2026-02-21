import { useState } from "react";
import ChatInterface from "../chat/ChatInterface";
import LogoutButton from "../auth/LogoutButton";
import AgentTopPanel from "../components/AgentTopPanel";
import AgentRentReceipt from "./AgentRentReceipt";

export default function AgentHome() {
    const userId = localStorage.getItem("userId") || "1";
    const email = localStorage.getItem("email") || "Agent";

    const [tab, setTab] = useState("rent");

    return (
        <div style={{ padding: 16 }}>
            <div
                style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: 12,
                }}
            >
                <div>
                    <h2 style={{ margin: 0 }}>Agent Dashboard</h2>
                </div>
                <LogoutButton />
            </div>

            <AgentTopPanel active={tab} onChange={setTab} />

            <div style={{ marginTop: 16 }}>
                {tab === "rent" && <AgentRentReceipt />}

                {tab === "chat" && (
                    <ChatInterface
                        userType="AGENT"
                        userId={parseInt(userId, 10)}
                        userName={email}
                    />
                )}
            </div>
        </div>
    );
}