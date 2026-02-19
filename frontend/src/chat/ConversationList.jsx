import './ConversationList.css';

export default function ConversationList({
                                             conversations,
                                             selectedConversation,
                                             onSelectConversation,
                                             userType,
                                             isLoading
                                         }) {

    return (
        <div className="conversation-list">

            {/* HEADER */}
            <div className="conversation-header">
                <h3>{userType === 'AGENT' ? 'Clients' : 'Agent'}</h3>
                <span className="conversation-count">
                    {conversations?.length || 0}
                </span>
            </div>


            <div className="conversations">


                {isLoading ? (
                    <div className="loading">
                        <p>Chargement des conversations...</p>
                    </div>
                ) : !conversations || conversations.length === 0 ? (


                    <div className="empty-conversations">
                        <p>Aucune conversation</p>
                    </div>

                ) : (


                    conversations.map((conv) => {

                        const displayName =
                            conv.otherUserName ||
                            `Utilisateur ${conv.otherUserId}`;

                        return (
                            <div
                                key={conv.id}
                                className={`conversation-item ${
                                    selectedConversation?.id === conv.id
                                        ? 'active'
                                        : ''
                                }`}
                                onClick={() => onSelectConversation(conv)}
                            >

                                <div className="conversation-avatar">
                                    {displayName.charAt(0).toUpperCase()}
                                </div>


                                <div className="conversation-details">
                                    <p className="conversation-name">
                                        {displayName}
                                    </p>

                                    <p className="conversation-preview">
                                        {conv.lastMessage || 'Pas de messages'}
                                    </p>
                                </div>


                                {conv.unreadCount > 0 && (
                                    <span className="unread-badge">
                                        {conv.unreadCount}
                                    </span>
                                )}

                            </div>
                        );
                    })
                )}

            </div>
        </div>
    );
}
