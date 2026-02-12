import ChatInterface from "../chat/ChatInterface";

export default function AgentHome() {
    const userId = localStorage.getItem("userId") || "1";
    const email = localStorage.getItem("email") || "Agent";

    return (
        <ChatInterface
            userType="AGENT"
            userId={parseInt(userId)}
            userName={email}
        />
    );
}