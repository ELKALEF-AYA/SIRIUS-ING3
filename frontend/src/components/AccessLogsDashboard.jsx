import { useState, useEffect } from 'react';
import axios from 'axios';
import '../styles/AccessLogsDashboard.css';

function AccessLogsDashboard() {
    const [logs, setLogs] = useState([]);
    const [grantedCount, setGrantedCount] = useState(0);
    const [deniedCount, setDeniedCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filterType, setFilterType] = useState('all');

    const API_URL = 'http://localhost:8081/api/auth/access';

    const fetchLogs = async () => {
        try {
            setLoading(true);
            setError(null);

            // Fetch all logs
            const allLogsRes = await axios.get(`${API_URL}/logs`);
            setLogs(allLogsRes.data);

            // Fetch granted count
            const grantedRes = await axios.get(`${API_URL}/logs/granted`);
            setGrantedCount(grantedRes.data.length);

            // Fetch denied count
            const deniedRes = await axios.get(`${API_URL}/logs/denied`);
            setDeniedCount(deniedRes.data.length);
        } catch (err) {
            setError(`Failed to fetch logs: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs();
        // Auto-refresh every 5 seconds
        const interval = setInterval(fetchLogs, 5000);
        return () => clearInterval(interval);
    }, []);

    const filteredLogs = filterType === 'all'
        ? logs
        : filterType === 'granted'
            ? logs.filter(log => log.accessGranted)
            : logs.filter(log => !log.accessGranted);

    return (
        <div className="dashboard-container">
            <div className="stats-grid">
                <div className="stat-card total">
                    <h3>Total Events</h3>
                    <p className="stat-number">{logs.length}</p>
                </div>
                <div className="stat-card granted">
                    <h3> Granted</h3>
                    <p className="stat-number">{grantedCount}</p>
                </div>
                <div className="stat-card denied">
                    <h3> Denied</h3>
                    <p className="stat-number">{deniedCount}</p>
                </div>
                <div className="stat-card percentage">
                    <h3>Success Rate</h3>
                    <p className="stat-number">
                        {logs.length > 0 ? ((grantedCount / logs.length) * 100).toFixed(1) : 0}%
                    </p>
                </div>
            </div>

            <div className="logs-section">
                <div className="logs-header">
                    <h2> Access Logs</h2>
                    <button className="refresh-btn" onClick={fetchLogs} disabled={loading}>
                        {loading ? ' Loading...' : ' Refresh'}
                    </button>
                </div>

                {error && <div className="error-message">{error}</div>}

                <div className="filter-buttons">
                    <button
                        className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
                        onClick={() => setFilterType('all')}
                    >
                        All ({logs.length})
                    </button>
                    <button
                        className={`filter-btn granted ${filterType === 'granted' ? 'active' : ''}`}
                        onClick={() => setFilterType('granted')}
                    >
                        Granted ({grantedCount})
                    </button>
                    <button
                        className={`filter-btn denied ${filterType === 'denied' ? 'active' : ''}`}
                        onClick={() => setFilterType('denied')}
                    >
                        Denied ({deniedCount})
                    </button>
                </div>

                {loading && filteredLogs.length === 0 ? (
                    <div className="loading-state">
                        <p>Loading access logs...</p>
                    </div>
                ) : filteredLogs.length === 0 ? (
                    <div className="empty-state">
                        <p>No access logs found</p>
                    </div>
                ) : (
                    <div className="logs-table-wrapper">
                        <table className="logs-table">
                            <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Status</th>
                                <th>Reason</th>
                                <th>Timestamp</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredLogs.map((log) => (
                                <tr key={log.id} className={log.accessGranted ? 'granted-row' : 'denied-row'}>
                                    <td className="log-id">{log.id}</td>
                                    <td className="log-name">
                                        {log.firstName} {log.lastName}
                                    </td>
                                    <td className="log-status">
                      <span className={`status-badge ${log.accessGranted ? 'granted' : 'denied'}`}>
                        {log.accessGranted ? ' Granted' : ' Denied'}
                      </span>
                                    </td>
                                    <td className="log-reason">{log.reason}</td>
                                    <td className="log-timestamp">
                                        {new Date(log.accessTimestamp).toLocaleString()}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}

export default AccessLogsDashboard;