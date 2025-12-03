import { useState } from 'react';
import axios from 'axios';
import '../styles/PublishEventForm.css';

function PublishEventForm() {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        accessGranted: true,
        reason: ''
    });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [messageType, setMessageType] = useState('');

    const API_URL = 'http://localhost:8081/api/auth/access';

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!formData.firstName.trim() || !formData.lastName.trim()) {
            setMessage('Please fill in all required fields');
            setMessageType('error');
            return;
        }

        try {
            setLoading(true);
            setMessage(null);

            const params = new URLSearchParams({
                firstName: formData.firstName,
                lastName: formData.lastName,
                accessGranted: formData.accessGranted,
                reason: formData.reason || 'Manual event'
            });

            const response = await axios.post(`${API_URL}/publish-event?${params}`);

            setMessage(' Event published successfully!');
            setMessageType('success');

            // Reset form
            setFormData({
                firstName: '',
                lastName: '',
                accessGranted: true,
                reason: ''
            });

            // Clear message after 3 seconds
            setTimeout(() => setMessage(null), 3000);
        } catch (err) {
            setMessage(` Error: ${err.response?.data?.message || err.message}`);
            setMessageType('error');
        } finally {
            setLoading(false);
        }
    };

    const handleQuickEvent = (firstName, lastName, granted, reason) => {
        setFormData({ firstName, lastName, accessGranted: granted, reason });
    };

    return (
        <div className="publish-container">
            <div className="form-card">
                <h2> Publish Access Event</h2>
                <p>Send a new access event to Kafka topic</p>

                {message && (
                    <div className={`message ${messageType}`}>
                        {message}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="event-form">
                    <div className="form-group">
                        <label htmlFor="firstName">First Name *</label>
                        <input
                            type="text"
                            id="firstName"
                            name="firstName"
                            value={formData.firstName}
                            onChange={handleChange}
                            placeholder="John"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="lastName">Last Name *</label>
                        <input
                            type="text"
                            id="lastName"
                            name="lastName"
                            value={formData.lastName}
                            onChange={handleChange}
                            placeholder="Doe"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="accessGranted">Access Status</label>
                        <div className="radio-group">
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    name="accessGranted"
                                    value="true"
                                    checked={formData.accessGranted === true}
                                    onChange={() => setFormData(prev => ({ ...prev, accessGranted: true }))}
                                />
                                Granted
                            </label>
                            <label className="radio-label">
                                <input
                                    type="radio"
                                    name="accessGranted"
                                    value="false"
                                    checked={formData.accessGranted === false}
                                    onChange={() => setFormData(prev => ({ ...prev, accessGranted: false }))}
                                />
                                 Denied
                            </label>
                        </div>
                    </div>



                    <button type="submit" className="submit-btn" disabled={loading}>
                        {loading ? ' Publishing...' : ' Publish Event'}
                    </button>
                </form>

                <div className="quick-actions">
                    <h3>Quick Actions</h3>
                    <div className="button-group">
                        <button
                            className="quick-btn granted"

                        >

                        </button>
                        <button
                            className="quick-btn denied"

                        >

                        </button>
                        <button
                            className="quick-btn granted"

                        >

                        </button>
                        <button
                            className="quick-btn denied"

                        >

                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default PublishEventForm;