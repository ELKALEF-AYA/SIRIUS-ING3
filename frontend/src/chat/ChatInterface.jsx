import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import ChatWindow from './ChatWindow';
import MessageInput from './MessageInput';
import ConversationList from './ConversationList';
import './ChatInterface.css';

const API_BASE_URL = 'http://localhost:8083/api/chat';
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



    useEffect(() => {
        const socket = new SockJS(WS_URL);
        const stompClient = Stomp.over(socket);

        stompClient.connect({}, () => {
            setIsConnected(true);

            const topic = `/topic/user/${userId}`;

            stompClient.subscribe(topic, (msg) => {
                const receivedMessage = JSON.parse(msg.body);


                if (selectedConversation &&
                    selectedConversation.id === receivedMessage.conversation.id) {

                    setMessages(prev => [...prev, receivedMessage]);

                } else {

                    setConversations(prev =>
                        prev.map(c =>
                            c.id === receivedMessage.conversation.id
                                ? { ...c, unreadCount: (c.unreadCount || 0) + 1 }
                                : c
                        )
                    );
                }


                setConversations(prev => {
                    const exists = prev.find(c => c.id === receivedMessage.conversation.id);
                    if (!exists) {
                        return [
                            ...prev,
                            {
                                id: receivedMessage.conversation.id,
                                otherUserId: receivedMessage.senderId,
                                otherUserName: receivedMessage.senderType === "CLIENT" ? "Client" : "Agent",
                                lastMessage: receivedMessage.content,
                                unreadCount: 1
                            }
                        ];
                    }
                    return prev;
                });


                updateConversationLastMessage(
                    receivedMessage.conversation.id,
                    receivedMessage.content
                );

                fetchConversations();
            });
        });

        stompClientRef.current = stompClient;

        return () => {
            if (stompClient.connected) {
                stompClient.disconnect();
            }
        };

    }, [userId, selectedConversation]);



    useEffect(() => {
        fetchConversations();
    }, []);

    const fetchConversations = async () => {
        const token = localStorage.getItem("accessToken");

        const response = await fetch(`${API_BASE_URL}/conversations`, {
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

        const token = localStorage.getItem("accessToken");

        const response = await fetch(
            `${API_BASE_URL}/conversations/${conversation.id}/messages`,
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        if (!response.ok) return;

        const data = await response.json();
        setMessages(data);

        setConversations(prev =>
            prev.map(c =>
                c.id === conversation.id
                    ? { ...c, unreadCount: 0 }
                    : c
            )
        );
    };



    const startConversation = async () => {
        if (!selectedClient) return;

        const token = localStorage.getItem("accessToken");

        const response = await fetch(
            `${API_BASE_URL}/conversations/start`,
            {
                method: "POST",
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ clientId: Number(selectedClient) })
            }
        );

        if (!response.ok) return;

        const conversation = await response.json();

        setConversations(prev => [...prev, conversation]);
        setSelectedConversation(conversation);

        setShowNewConversation(false);
        setSelectedClient("");
    };

    const handleSendMessage = (content) => {
        if (!selectedConversation) return;

        const message = {
            conversationId: selectedConversation.id,
            senderId: userId,
            senderType: userType,
            content: content
        };


        stompClientRef.current.send(
            '/app/chat.send',
            {},
            JSON.stringify(message)
        );

        if (userType === "AGENT") {
            setMessages(prev => [
                ...prev,
                {
                    id: Date.now(),
                    content: content,
                    senderId: userId,
                    receiverId: selectedConversation.otherUserId,
                    senderType: userType,
                    timestamp: new Date().toISOString(),
                    conversation: { id: selectedConversation.id }
                }
            ]);

            updateConversationLastMessage(selectedConversation.id, content);
        }
    };



    const updateConversationLastMessage = (conversationId, lastMessage) => {
        setConversations(prev =>
            prev.map(conv =>
                conv.id === conversationId
                    ? { ...conv, lastMessage }
                    : conv
            )
        );
    };



    return (
        <div className="chat-interface">

            <div style={{ padding: 10 }}>
                {userType === "AGENT" && (
                    <>
                        <button
                            onClick={() => {
                                fetchClients();
                                setShowNewConversation(true);
                            }}
                        >
                            + Nouvelle conversation
                        </button>

                        {showNewConversation && (
                            <div style={{ marginTop: 10 }}>
                                <select
                                    value={selectedClient}
                                    onChange={(e) => setSelectedClient(e.target.value)}
                                >
                                    <option value="">Choisir un client</option>
                                    {clients.map(client => (
                                        <option key={client.id} value={client.id}>
                                            {client.fullName}
                                        </option>
                                    ))}
                                </select>

                                <button onClick={startConversation}>
                                    Démarrer
                                </button>
                            </div>
                        )}
                    </>
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
                        <ChatWindow
                            messages={messages}
                            currentUserId={userId}
                        />

                        <MessageInput
                            onSendMessage={handleSendMessage}
                            disabled={!isConnected}
                        />
                    </>
                ) : (
                    <div className="no-conversation">
                        <p>Sélectionnez une conversation</p>
                    </div>
                )}
            </div>
        </div>
    );
}
