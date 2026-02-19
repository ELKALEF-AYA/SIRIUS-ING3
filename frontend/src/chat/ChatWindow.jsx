import { useEffect, useRef } from 'react';
import './ChatWindow.css';

export default function ChatWindow({ messages, currentUserId, isSending }) {

    const messagesEndRef = useRef(null);


    const scrollToBottom = () => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    return (
        <div className="chat-window">
            <div className="messages-container">


                {messages.length === 0 ? (
                    <div className="empty-chat">
                        <p>Aucun message pour le moment</p>
                    </div>
                ) : (
                    messages.map((msg) => (
                        <div
                            key={msg.id}
                            className={`message ${
                                msg.senderId === currentUserId ? 'sent' : 'received'
                            }`}
                        >
                            <div className="message-content">
                                <p>{msg.content}</p>
                            </div>

                            <div className="message-time">
                                {new Date(msg.timestamp).toLocaleTimeString('fr-FR', {
                                    hour: '2-digit',
                                    minute: '2-digit',
                                })}
                            </div>
                        </div>
                    ))
                )}


                {isSending && (
                    <div className="message sent sending">
                        <div className="message-content">
                            <p>Envoi...</p>
                        </div>
                    </div>
                )}

                <div ref={messagesEndRef} />
            </div>
        </div>
    );
}
