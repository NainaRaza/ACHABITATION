import { state } from "./state.js";

export const API_BASE_URL = window.ACHABITATION_API_BASE_URL
    || localStorage.getItem("achabitation.apiBaseUrl")
    || "http://localhost:8080/api/v1";
export const API = API_BASE_URL.replace(/\/$/, "");

export function accessToken() {
    return state.user?.accessToken || state.user?.sessionToken || null;
}

export async function api(path, options = {}) {
    const token = accessToken();
    const authHeaders = token ? { "Authorization": `Bearer ${token}` } : {};
    const response = await fetch(`${API}${path}`, {
        headers: { "Content-Type": "application/json", ...authHeaders, ...(options.headers || {}) },
        ...options
    });

    if (response.status === 401) {
        throw new Error("Session expirée ou token invalide. Reconnecte-toi.");
    }

    if (!response.ok) {
        let details = `${response.status} ${response.statusText}`;
        try {
            const body = await response.json();
            if (body.details && Array.isArray(body.details)) {
                details = body.details.join("\n");
            } else if (body.message) {
                details = body.message;
            } else if (body.error) {
                details = `${body.error}${body.path ? ` - ${body.path}` : ""}`;
            } else {
                details = JSON.stringify(body);
            }
        } catch (_) {
            const text = await response.text().catch(() => "");
            if (text) details = text;
        }
        throw new Error(details);
    }

    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

export async function fetchBlob(path) {
    const token = accessToken();
    const response = await fetch(`${API}${path}`, {
        headers: token ? { "Authorization": `Bearer ${token}` } : {}
    });
    if (response.status === 401) {
        throw new Error("Session expirée ou token invalide. Reconnecte-toi.");
    }
    if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.details?.join("\n") || `${response.status} ${response.statusText}`);
    }
    return response.blob();
}
