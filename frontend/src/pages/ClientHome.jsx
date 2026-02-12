import ChatInterface from "../chat/ChatInterface";

export default function ClientHome() {
    const userId = localStorage.getItem("userId") || "1";
    const email = localStorage.getItem("email") || "Client";

    return (
        <ChatInterface
            userType="CLIENT"
            userId={parseInt(userId)}
            userName={email}
        />
    );
}