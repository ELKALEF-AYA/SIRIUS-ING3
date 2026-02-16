import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import ChatWindow from './ChatWindow';
import MessageInput from './MessageInput';
import ConversationList from './ConversationList';
import './ChatInterface.css';

export default function ChatInterface({ userType, userId, userName }) {
    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);
    const [isSending, setIsSending] = useState(false);
    const [isConnected, setIsConnected] = useState(false);
    const stompClientRef = useRef(null);

    useEffect(() => {
        const socket = new SockJS('http://localhost:8081/ws');
        const stompClient = Stomp.over(socket);

        stompClient.connect(
            {},
            () => {
                console.log('✓ WebSocket connecté');
                setIsConnected(true);

                const topic =
                    userType === 'AGENT'
                        ? `/topic/agent/${userId}`
                        : `/topic/client/${userId}`;

                stompClient.subscribe(topic, (message) => {
                    const receivedMessage = JSON.parse(message.body);
                    console.log('Message reçu:', receivedMessage);

                    setMessages((prev) => [...prev, receivedMessage]);
                    updateConversationLastMessage(
                        receivedMessage.senderId,
                        receivedMessage.content
                    );
                });
            },
            (error) => {
                console.error('Erreur WebSocket:', error);
                setIsConnected(false);
            }
        );

        stompClientRef.current = stompClient;

        return () => {
            if (stompClient && stompClient.connected) {
                stompClient.disconnect();
            }
        };
    }, [userType, userId]);

    useEffect(() => {
        const dummyConversations =
            userType === 'AGENT'
                ? [
                    { id: 1, name: 'Client 1', lastMessage: '', unreadCount: 0 },
                    { id: 2, name: 'Client 2', lastMessage: '', unreadCount: 2 },
                    { id: 3, name: 'Client 3', lastMessage: '', unreadCount: 0 },
                ]
                : [
                    { id: 1, name: 'Agent Support', lastMessage: '', unreadCount: 0 },
                ];
        setConversations(dummyConversations);
    }, [userType]);

    const handleSelectConversation = (conversation) => {
        setSelectedConversation(conversation);
        setMessages([]);
    };

    const handleSendMessage = (content) => {
        if (!selectedConversation || !stompClientRef.current) {
            alert('Veuillez sélectionner une conversation');
            return;
        }

        const message = {
            senderId: userId,
            receiverId: selectedConversation.id,
            senderType: userType,
            content: content,
            conversationId: selectedConversation.id,
            timestamp: new Date().toISOString(),
        };

        setIsSending(true);

        stompClientRef.current.send('/app/chat.send', {}, JSON.stringify(message));

        setTimeout(() => {
            setMessages((prev) => [...prev, message]);
            setIsSending(false);
        }, 300);
    };

    const updateConversationLastMessage = (conversationId, lastMessage) => {
        setConversations((prev) =>
            prev.map((conv) =>
                conv.id === conversationId
                    ? { ...conv, lastMessage, unreadCount: 0 }
                    : conv
            )
        );
    };

    return (
        <div className="chat-interface">
            <ConversationList
                conversations={conversations}
                selectedConversation={selectedConversation}
                onSelectConversation={handleSelectConversation}
                userType={userType}
            />

            <div className="chat-main">
                {selectedConversation ? (
                    <>
                        <div className="chat-header">
                            <div className="chat-header-info">
                                <h2>{selectedConversation.name}</h2>
                                <span className={`status ${isConnected ? 'connected' : 'disconnected'}`}>
                                    {isConnected ? '● En ligne' : '● Hors ligne'}
                                </span>
                            </div>
                        </div>

                        <ChatWindow
                            messages={messages}
                            currentUserId={userId}
                            isSending={isSending}
                        />

                        <MessageInput
                            onSendMessage={handleSendMessage}
                            disabled={!isConnected}
                        />
                    </>
                ) : (
                    <div className="no-conversation">
                        <p>Sélectionnez une conversation pour commencer</p>
                    </div>
                )}
            </div>
        </div>
    );
}