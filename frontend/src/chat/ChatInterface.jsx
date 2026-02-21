import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import ChatWindow from './ChatWindow';
import MessageInput from './MessageInput';
import ConversationList from './ConversationList';
import './ChatInterface.css';

const API_BASE_URL = 'http://localhost:8080/api/chat';
const WS_URL = 'http://localhost:8083/ws';

export default function ChatInterface({ userType, userId }) {

    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);
    const [isConnected, setIsConnected] = useState(false);
    const [clients, setClients] = useState([]);
    const [selectedClient, setSelectedClient] = useState("");
    const [showNewConversation, setShowNewConversation] = useState(false);

    const stompClientRef = useRef(null);
    const selectedConversationRef = useRef(null);

    useEffect(() => {
        selectedConversationRef.current = selectedConversation;
    }, [selectedConversation]);

    useEffect(() => {
        const socket = new SockJS(WS_URL);
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            setIsConnected(true);

            stompClient.subscribe(`/topic/user/${userId}`, (msg) => {
                const received = JSON.parse(msg.body);

                if (received.type === 'NEW_CONVERSATION') {
                    fetchConversations();
                    return;
                }

                const currentConv = selectedConversationRef.current;

                if (currentConv && currentConv.id === received.conversationId) {
                    setMessages(prev => {
                        const exists = prev.find(m => m.id === received.id);
                        if (exists) return prev;
                        return [...prev, received];
                    });
                } else {
                    setConversations(prev =>
                        prev.map(c =>
                            c.id === received.conversationId
                                ? { ...c, unreadCount: (c.unreadCount || 0) + 1, lastMessage: received.content }
                                : c
                        )
                    );
                }

                fetchConversations();
            });
        });

        stompClientRef.current = stompClient;

        return () => {
            if (stompClient.connected) stompClient.disconnect();
        };
    }, [userId]);

    useEffect(() => {
        fetchConversations();
    }, []);

    const fetchConversations = async () => {
        const token = localStorage.getItem("accessToken");
        const endpoint = userType === 'AGENT' ? '/conversations' : '/my-conversations';

        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) return;
        const data = await response.json();
        setConversations(data);
    };

    const fetchClients = async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_BASE_URL}/clients`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) return;
        const data = await response.json();
        setClients(data);
    };

    const handleSelectConversation = async (conversation) => {
        setSelectedConversation(conversation);
        setMessages([]);

        const token = localStorage.getItem("accessToken");
        const response = await fetch(
            `${API_BASE_URL}/conversations/${conversation.id}/messages`,
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (!response.ok) return;
        const data = await response.json();
        setMessages(data);

        setConversations(prev =>
            prev.map(c => c.id === conversation.id ? { ...c, unreadCount: 0 } : c)
        );
    };

    const startConversation = async () => {
        if (!selectedClient) return;

        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_BASE_URL}/conversations/start`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ clientId: Number(selectedClient) })
        });

        if (!response.ok) return;
        const conversation = await response.json();

        setConversations(prev => {
            const exists = prev.find(c => c.id === conversation.id);
            if (exists) return prev;
            return [...prev, conversation];
        });

        handleSelectConversation(conversation);
        setShowNewConversation(false);
        setSelectedClient("");
    };

    const startClientConversation = async () => {
        const token = localStorage.getItem("accessToken");
        const response = await fetch(`${API_BASE_URL}/conversations/start`, {
            method: "POST",
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ clientId: Number(userId) })
        });

        if (!response.ok) return;
        const conversation = await response.json();

        setConversations(prev => {
            const exists = prev.find(c => c.id === conversation.id);
            if (exists) return prev;
            return [...prev, conversation];
        });

        handleSelectConversation(conversation);
    };

    const handleSendMessage = (content) => {
        if (!selectedConversation || !stompClientRef.current?.connected) return;

        stompClientRef.current.send('/app/chat.send', {}, JSON.stringify({
            conversationId: selectedConversation.id,
            senderId: userId,
            senderType: userType,
            content: content
        }));
    };

    return (
        <div className="chat-interface">
            <div style={{ padding: 10 }}>
                {userType === "AGENT" && (
                    <>
                        <button onClick={() => { fetchClients(); setShowNewConversation(true); }}>
                            + Nouvelle conversation
                        </button>

                        {showNewConversation && (
                            <div style={{ marginTop: 10 }}>
                                <select value={selectedClient} onChange={(e) => setSelectedClient(e.target.value)}>
                                    <option value="">Choisir un client</option>
                                    {clients.map(client => (
                                        <option key={client.id} value={client.id}>{client.fullName}</option>
                                    ))}
                                </select>
                                <button onClick={startConversation}>Démarrer</button>
                            </div>
                        )}
                    </>
                )}

                {userType === "CLIENT" && conversations.length === 0 && (
                    <button onClick={startClientConversation}>
                        + Contacter l'agent
                    </button>
                )}
            </div>

            <ConversationList
                conversations={conversations}
                selectedConversation={selectedConversation}
                onSelectConversation={handleSelectConversation}
                userType={userType}
            />

            <div className="chat-main">
                {selectedConversation ? (
                    <>
                        <ChatWindow messages={messages} currentUserId={userId} />
                        <MessageInput onSendMessage={handleSendMessage} disabled={!isConnected} />
                    </>
                ) : (
                    <div className="no-conversation"><p>Sélectionnez une conversation</p></div>
                )}
            </div>
        </div>
    );
}