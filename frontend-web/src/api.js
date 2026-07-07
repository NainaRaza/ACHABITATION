import { state } from "./state.js";

export const API_BASE_URL = window.ACHABITATION_API_BASE_URL
    || localStorage.getItem("achabitation.apiBaseUrl")
    || "http://localhost:8080/api/v1";
export const API = API_BASE_URL.replace(/\/$/, "");

const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";

export function accessToken() {
    return state.user?.accessToken || state.user?.sessionToken || null;
}

export function sanitizeAuthUser(user) {
    if (!user) return null;
    const { accessToken, sessionToken, devToken, ...safeUser } = user;
    return safeUser;
}

function isMutatingMethod(method) {
    return !["GET", "HEAD", "OPTIONS"].includes((method || "GET").toUpperCase());
}

function readCookie(name) {
    const prefix = `${encodeURIComponent(name)}=`;
    const cookieString = typeof document !== "undefined" && typeof document.cookie === "string" ? document.cookie : "";
    return cookieString
        .split(";")
        .map(part => part.trim())
        .find(part => part.startsWith(prefix))
        ?.slice(prefix.length) || null;
}

async function ensureCsrfToken() {
    let token = readCookie(CSRF_COOKIE_NAME);
    if (token) return decodeURIComponent(token);
    await fetch(`${API}/auth/csrf`, {
        method: "GET",
        credentials: "include",
        headers: { "Accept": "application/json" }
    });
    token = readCookie(CSRF_COOKIE_NAME);
    return token ? decodeURIComponent(token) : null;
}

export async function api(path, options = {}) {
    const token = accessToken();
    const authHeaders = token ? { "Authorization": `Bearer ${token}` } : {};
    const method = (options.method || "GET").toUpperCase();
    const csrfHeaders = {};
    if (isMutatingMethod(method) && !options.headers?.[CSRF_HEADER_NAME]) {
        const csrfToken = await ensureCsrfToken();
        if (csrfToken) csrfHeaders[CSRF_HEADER_NAME] = csrfToken;
    }

    const response = await fetch(`${API}${path}`, {
        credentials: "include",
        headers: { "Content-Type": "application/json", ...authHeaders, ...csrfHeaders, ...(options.headers || {}) },
        ...options,
        method
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
        credentials: "include",
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
