import { useEffect, useRef } from 'react';
import './ChatWindow.css';

export default function ChatWindow({ messages, currentUserId }) {

    const messagesEndRef = useRef(null);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    return (
        <div className="chat-window">
            <div className="messages-container">
                {messages.length === 0 ? (
                    <div className="empty-chat"><p>Aucun message pour le moment</p></div>
                ) : (
                    messages.map((msg) => {
                        const isMine = Number(msg.senderId) === Number(currentUserId);
                        return (
                            <div key={msg.id} className={`message ${isMine ? 'sent' : 'received'}`}>
                                <div className="message-content">
                                    <p>{msg.content}</p>
                                </div>
                                <div className="message-time">
                                    {new Date(msg.timestamp).toLocaleTimeString('fr-FR', {
                                        hour: '2-digit',
                                        minute: '2-digit'
                                    })}
                                </div>
                            </div>
                        );
                    })
                )}
                <div ref={messagesEndRef} />
            </div>
        </div>
    );
}