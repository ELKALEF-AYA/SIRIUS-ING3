import { useState, useEffect } from 'react';
import './App.css';

function App() {
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [message, setMessage] = useState('');
    const [messageType, setMessageType] = useState('');
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(false);

    const API_URL = 'auth/access';

    useEffect(() => {
        // Fetch history on load
        fetchHistory();
    }, []);

    const fetchHistory = async () => {
        try {
            const response = await fetch(`${API_URL}/logs`);
            const data = await response.json();
            setHistory(data);
        } catch (err) {
            console.error('Error fetching history:', err);
        }
    };

    const handleValidate = async (e) => {
        e.preventDefault();

        if (!firstName.trim() || !lastName.trim()) {
            setMessage(' Please enter first name and last name');
            setMessageType('warning');
            return;
        }

        try {
            setLoading(true);
            setMessage('');

            const response = await fetch(
                `${API_URL}/validate?firstName=${encodeURIComponent(firstName)}&lastName=${encodeURIComponent(lastName)}`
            );

            const data = await response.json();

            if (data.accessGranted) {
                setMessage(` Access Granted - ${data.reason}`);
                setMessageType('accepted');
            } else {
                setMessage(` Access Denied - ${data.reason}`);
                setMessageType('refused');
            }

            // Clear inputs
            setFirstName('');
            setLastName('');

            // Refresh history
            setTimeout(fetchHistory, 500);
        } catch (err) {
            setMessage(' Error: ' + err.message);
            setMessageType('error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="app-container">
            <header className="header">
                <h1>JSA Home Access Control</h1>
                <p>Badge Validation System</p>
            </header>

            <main className="main-content">
                <section className="validation-section">
                    <div className="form-card">
                        <h2>Validate Access</h2>

                        <form onSubmit={handleValidate} className="form">
                            <div className="form-group">
                                <label htmlFor="firstName">First Name</label>
                                <input
                                    type="text"
                                    id="firstName"
                                    value={firstName}
                                    onChange={(e) => setFirstName(e.target.value)}
                                    placeholder="John"
                                    disabled={loading}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="lastName">Last Name</label>
                                <input
                                    type="text"
                                    id="lastName"
                                    value={lastName}
                                    onChange={(e) => setLastName(e.target.value)}
                                    placeholder="Doe"
                                    disabled={loading}
                                />
                            </div>

                            <button
                                type="submit"
                                className="validate-btn"
                                disabled={loading}
                            >
                                {loading ? 'Validating...' : '✓ Validate'}
                            </button>
                        </form>

                        {message && (
                            <div className={`message ${messageType}`}>
                                {message}
                            </div>
                        )}
                    </div>
                </section>

                <section className="history-section">
                    <div className="history-card">
                        <h2>Access History</h2>

                        {history.length === 0 ? (
                            <div className="empty-state">
                                <p>No access attempts yet</p>
                            </div>
                        ) : (
                            <div className="history-list">
                                {history.map((entry) => (
                                    <div key={entry.id} className={`history-item ${entry.accessGranted ? 'accepted' : 'refused'}`}>
                                        <div className="history-status">
                                            {entry.accessGranted ? '' : ''}
                                        </div>
                                        <div className="history-info">
                                            <p className="history-name">
                                                <strong>{entry.firstName} {entry.lastName}</strong>
                                            </p>
                                            <p className="history-reason">{entry.reason}</p>
                                            <p className="history-time">
                                                {new Date(entry.accessTimestamp).toLocaleString()}
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </section>
            </main>

            <footer className="footer">
                <p>JSA Home v1.0.0 | Real-time Access Control System</p>
            </footer>
        </div>
    );
}

export default App;