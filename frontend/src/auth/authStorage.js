export function saveAuth(data) {
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("role", data.role);
    localStorage.setItem("userId", String(data.userId));
    localStorage.setItem("tenantId", String(data.tenantId));
    localStorage.setItem("email", data.email);
    localStorage.setItem("firstName", data.firstName ?? "");
    localStorage.setItem("lastName", data.lastName ?? "");
}

export function clearAuth() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("role");
    localStorage.removeItem("userId");
    localStorage.removeItem("tenantId");
    localStorage.removeItem("email");
    localStorage.removeItem("firstName");
    localStorage.removeItem("lastName");
}

export function isLoggedIn() {
    return !!localStorage.getItem("accessToken");
}

export function getRole() {
    return localStorage.getItem("role");
}
export function getFullName() {
    const first = localStorage.getItem("firstName") || "";
    const last = localStorage.getItem("lastName") || "";
    return `${first} ${last}`.trim();
}