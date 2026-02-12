import { useState, useRef } from 'react';
import './MessageInput.css';

export default function MessageInput({ onSendMessage, disabled }) {
    const [message, setMessage] = useState('');
    const textareaRef = useRef(null);

    const handleInputChange = (e) => {
        setMessage(e.target.value);
        e.target.style.height = 'auto';
        e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px';
    };

    const handleSendMessage = () => {
        if (message.trim() && !disabled) {
            onSendMessage(message.trim());
            setMessage('');
            if (textareaRef.current) {
                textareaRef.current.style.height = 'auto';
            }
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    return (
        <div className="message-input-container">
            <textarea
                ref={textareaRef}
                className="message-input"
                value={message}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
                placeholder="Écrivez un message..."
                disabled={disabled}
                rows={1}
            />
            <button
                className="send-button"
                onClick={handleSendMessage}
                disabled={disabled || !message.trim()}
                title="Entrée pour envoyer, Maj+Entrée pour nouvelle ligne"
            >
                <span>➤</span>
            </button>
        </div>
    );
}