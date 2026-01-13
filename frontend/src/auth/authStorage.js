export function saveAuth(data) {
    localStorage.setItem("accessToken", data.accessToken);
    localStorage.setItem("role", data.role);
    localStorage.setItem("userId", String(data.userId));
    localStorage.setItem("email", data.email);
}

export function clearAuth() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("role");
    localStorage.removeItem("userId");
    localStorage.removeItem("email");
}

export function isLoggedIn() {
    return !!localStorage.getItem("accessToken");
}

export function getRole() {
    return localStorage.getItem("role");
}