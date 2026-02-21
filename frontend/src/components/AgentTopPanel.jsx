export default function AgentTopPanel({ active, onChange }) {
    const TabButton = ({ id, label }) => (
        <button
            type="button"
            onClick={() => onChange(id)}
            style={{
                padding: "10px 12px",
                borderRadius: 10,
                border: "1px solid #ddd",
                background: active === id ? "#f2f2f2" : "white",
                cursor: "pointer",
            }}
        >
            {label}
        </button>
    );

    return (
        <div style={{ display: "flex", gap: 10, marginTop: 16 }}>
            <TabButton id="rent" label="Rent receipt" />
            <TabButton id="chat" label="Chat" />
        </div>
    );
}