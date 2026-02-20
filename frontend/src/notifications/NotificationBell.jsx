import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useNotifications } from "./useNotifications";
import "./notifications.css";

export default function NotificationBell() {
    const { items, unreadCount, connected, markRead, markAllRead } = useNotifications();
    const [open, setOpen] = useState(false);
    const [filter, setFilter] = useState("all");
    const ref = useRef(null);
    const navigate = useNavigate();

    function formatDateTime(iso) {
        if (!iso) return "";
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return "";
        return d.toLocaleString("fr-FR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    useEffect(() => {
        function onDocClick(e) {
            if (!ref.current) return;
            if (!ref.current.contains(e.target)) setOpen(false);
        }
        document.addEventListener("mousedown", onDocClick);
        return () => document.removeEventListener("mousedown", onDocClick);
    }, []);

    function onClickNotif(n) {
        if (!n.isRead) markRead(n.id);

        if (n.link) {
            const sep = n.link.includes("?") ? "&" : "?";
            navigate(`${n.link}${sep}t=${Date.now()}`);
        }

        setOpen(false);
    }

    const visibleItems = useMemo(() => {
        const sorted = [...items].sort((a, b) => {
            if (a.isRead !== b.isRead) return a.isRead ? 1 : -1;
            const ta = Date.parse(a.createdAt) || 0;
            const tb = Date.parse(b.createdAt) || 0;
            return tb - ta;
        });

        if (filter === "unread") return sorted.filter((n) => !n.isRead);
        return sorted;
    }, [items, filter]);

    function onKeyDownNotif(e, n) {
        if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onClickNotif(n);
        }
    }

    return (
        <div className="notif-root" ref={ref}>
            <button
                className="notif-bell"
                onClick={() => setOpen((v) => !v)}
                title="Notifications"
                type="button"
            >
                <img src="/notification.png" alt="Notifications" className="notif-bell-icon" />
                {unreadCount > 0 && <span className="notif-badge">{unreadCount}</span>}
            </button>

            {open && (
                <div className="notif-panel">
                    <div className="notif-header">
                        <div>
                            <div className="notif-title">Notifications</div>
                            <div className="notif-subtitle">{connected ? "Connecté" : "Déconnecté"}</div>
                        </div>

                        <button
                            className="notif-link"
                            onClick={markAllRead}
                            disabled={unreadCount === 0}
                            type="button"
                        >
                            Tout marquer comme lu
                        </button>
                    </div>

                    <div className="notif-filters">
                        <button
                            type="button"
                            className={`notif-filter ${filter === "all" ? "active" : ""}`}
                            onClick={() => setFilter("all")}
                        >
                            Tous
                        </button>

                        <button
                            type="button"
                            className={`notif-filter ${filter === "unread" ? "active" : ""}`}
                            onClick={() => setFilter("unread")}
                        >
                            Non lus ({unreadCount})
                        </button>
                    </div>

                    <div className="notif-list">
                        {visibleItems.length === 0 ? (
                            <div className="notif-empty">
                                {filter === "unread" ? "Aucune notification non lue" : "Aucune notification"}
                            </div>
                        ) : (
                            visibleItems.map((n) => (
                                <div
                                    key={n.id}
                                    className={`notif-item ${n.isRead ? "" : "unread"}`}
                                    onClick={() => onClickNotif(n)}
                                    onKeyDown={(e) => onKeyDownNotif(e, n)}
                                    role="button"
                                    tabIndex={0}
                                >
                                    <div className="notif-item-top">
                                        <div className="notif-item-title">{n.title}</div>
                                        <div className="notif-item-date">{formatDateTime(n.createdAt)}</div>
                                    </div>

                                    <div className="notif-item-body">
                                        {(() => {
                                            const body = n.body || "";
                                            const cta = "Cliquez pour la télécharger.";
                                            if (!body.includes(cta)) return body;

                                            const before = body.replace(cta, "").trim();
                                            return (
                                                <>
                                                    {before}{" "}
                                                    <span className="notif-cta">{cta}</span>
                                                </>
                                            );
                                        })()}
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}