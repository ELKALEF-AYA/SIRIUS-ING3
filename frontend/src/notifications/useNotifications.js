import { useEffect, useRef, useState } from "react";
import { NotificationsApi } from "./notificationsApi";

export function useNotifications() {
    const [items, setItems] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [connected, setConnected] = useState(false);
    const esRef = useRef(null);

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                const [listRes, countRes] = await Promise.all([
                    NotificationsApi.my(),
                    NotificationsApi.unreadCount(),
                ]);

                if (cancelled) return;
                setItems(listRes.data || []);
                setUnreadCount(countRes.data?.unreadCount ?? 0);
            } catch (e) {
                console.log("Notification load error:", e?.response?.status, e);
            }
        }

        load();
        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        if (!token) return;

        const url = `/api/notifications/stream?token=${encodeURIComponent(token)}`;
        const es = new EventSource(url);
        esRef.current = es;

        es.onopen = () => setConnected(true);
        es.onerror = () => setConnected(false);

        const onAnyEvent = (event) => {
            try {
                if (event.data === "ok") return;

                const notif = JSON.parse(event.data);

                setItems((prev) => {
                    if (prev.some((x) => x.id === notif.id)) return prev;
                    return [notif, ...prev];
                });

                if (notif?.isRead === false) {
                    setUnreadCount((c) => c + 1);
                }
            } catch (err) {
                console.log("SSE parse error", err, event?.data);
            }
        };

        es.addEventListener("notification", onAnyEvent);

        es.onmessage = onAnyEvent;

        return () => {
            es.close();
            esRef.current = null;
        };
    }, []);

    async function markRead(id) {
        const res = await NotificationsApi.readOne(id);
        const updated = res.data;

        setItems((prev) => {
            const wasUnread = prev.find((n) => n.id === id)?.isRead === false;

            const next = prev.map((n) => (n.id === id ? updated : n));

            if (wasUnread && updated?.isRead === true) {
                setUnreadCount((c) => Math.max(0, c - 1));
            }

            return next;
        });

        return updated;
    }

    async function markAllRead() {
        await NotificationsApi.readAll();
        setItems((prev) => prev.map((n) => ({ ...n, isRead: true })));
        setUnreadCount(0);
    }

    return { items, unreadCount, connected, markRead, markAllRead };
}