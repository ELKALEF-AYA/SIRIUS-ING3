
import ChatInterface from "../chat/ChatInterface";

export default function ClientHome() {
    const userId = localStorage.getItem("userId") || "1";
    const email = localStorage.getItem("email") || "Client";

    return (
        <ChatInterface
            userType="CLIENT"
            userId={parseInt(userId)}
            userName={email}

import LogoutButton from "../auth/LogoutButton";
import { getFullName } from "../auth/authStorage";

export default function ClientHome() {
    const fullName = getFullName();


    return (
        <div style={{ padding: 16 }}>
            <h2>Espace Client</h2>
            <p>Connecté : {fullName || "Client"}</p>
            <LogoutButton />
        </div>

    );
}