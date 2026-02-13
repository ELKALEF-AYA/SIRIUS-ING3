import './ConversationList.css';

export default function ConversationList({ conversations, selectedConversation, onSelectConversation, userType }) {
    return (
        <div className="conversation-list">
            <div className="conversation-header">
                <h3>{userType === 'AGENT' ? 'Clients' : 'Agents'}</h3>
                <span className="conversation-count">{conversations.length}</span>
            </div>
            <div className="conversations">
                {conversations.length === 0 ? (
                    <div className="empty-conversations">
                        <p>Aucune conversation</p>
                    </div>
                ) : (
                    conversations.map((conv) => (
                        <div
                            key={conv.id}
                            className={`conversation-item ${
                                selectedConversation?.id === conv.id ? 'active' : ''
                            }`}
                            onClick={() => onSelectConversation(conv)}
                        >
                            <div className="conversation-avatar">
                                {conv.name.charAt(0).toUpperCase()}
                            </div>
                            <div className="conversation-details">
                                <p className="conversation-name">{conv.name}</p>
                                <p className="conversation-preview">
                                    {conv.lastMessage || 'Pas de messages'}
                                </p>
                            </div>
                            {conv.unreadCount > 0 && (
                                <span className="unread-badge">{conv.unreadCount}</span>
                            )}
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}