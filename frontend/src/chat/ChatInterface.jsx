import { useEffect, useState, useRef } from 'react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import ChatWindow from './ChatWindow';
import MessageInput from './MessageInput';
import ConversationList from './ConversationList';
import './ChatInterface.css';

const API_BASE_URL = 'http://localhost:8083/api/chat';
const WS_URL = 'http://localhost:8083/ws';

export default function ChatInterface({ userType, userId, userName }) {
    const [conversations, setConversations] = useState([]);
    const [selectedConversation, setSelectedConversation] = useState(null);
    const [messages, setMessages] = useState([]);
    const [isSending, setIsSending] = useState(false);
    const [isConnected, setIsConnected] = useState(false);
    const [isLoadingConversations, setIsLoadingConversations] = useState(true);
    const [isLoadingMessages, setIsLoadingMessages] = useState(false);
    const stompClientRef = useRef(null);


    useEffect(() => {
        console.log('Connecting to WebSocket...');
        const socket = new SockJS(WS_URL);
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

                console.log('Subscribing to topic:', topic);
                stompClient.subscribe(topic, (message) => {
                    const receivedMessage = JSON.parse(message.body);
                    console.log('Message reçu:', receivedMessage);

                    setMessages((prev) => [...prev, receivedMessage]);
                    updateConversationLastMessage(
                        receivedMessage.conversationId,
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
        fetchConversations();
    }, [userId]);

    const fetchConversations = async () => {
        try {
            setIsLoadingConversations(true);
            console.log('Fetching conversations for userId:', userId);

            const response = await fetch(`${API_BASE_URL}/conversations?userId=${userId}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            console.log('Conversations reçues:', data);

            const formattedConversations = data.map((conv) => ({
                id: conv.id,
                otherUserId: conv.otherUserId,
                name: `User ${conv.otherUserId}`,
                lastMessage: conv.lastMessage || 'Pas de messages',
                lastMessageTime: conv.lastMessageTime,
                unreadCount: conv.unreadCount || 0,
            }));

            setConversations(formattedConversations);
        } catch (error) {
            console.error('Erreur lors du fetch des conversations:', error);
            setConversations([]);
        } finally {
            setIsLoadingConversations(false);
        }
    };


    const handleSelectConversation = async (conversation) => {
        console.log('Selection conversation:', conversation);
        setSelectedConversation(conversation);
        setMessages([]);
        setIsLoadingMessages(true);

        try {
            console.log(`Fetching messages for conversationId ${conversation.id}`);
            const response = await fetch(
                `${API_BASE_URL}/conversations/${conversation.id}/messages?userId=${userId}`
            );

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            console.log('Messages reçus:', data);
            setMessages(data);
        } catch (error) {
            console.error('Erreur lors du fetch des messages:', error);
            setMessages([]);
        } finally {
            setIsLoadingMessages(false);
        }
    };


    const handleSendMessage = (content) => {
        if (!selectedConversation || !stompClientRef.current) {
            alert('Veuillez sélectionner une conversation');
            return;
        }

        const message = {
            senderId: userId,
            receiverId: selectedConversation.otherUserId,
            senderType: userType,
            content: content,
            conversationId: selectedConversation.id,
            timestamp: new Date().toISOString(),
        };

        console.log('Envoi du message:', message);
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
                isLoading={isLoadingConversations}
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

                        {isLoadingMessages ? (
                            <div className="loading">
                                <p>Chargement des messages...</p>
                            </div>
                        ) : (
                            <ChatWindow
                                messages={messages}
                                currentUserId={userId}
                                isSending={isSending}
                            />
                        )}

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