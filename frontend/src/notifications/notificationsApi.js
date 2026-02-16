import { api } from "../api/api";

export const NotificationsApi = {
    my: () => api.get("/notifications/me"),
    unreadCount: () => api.get("/notifications/me/unread-count"),
    readOne: (id) => api.patch(`/notifications/${id}/read`),
    readAll: () => api.patch("/notifications/me/read-all"),
};