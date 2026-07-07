export function readJson(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : fallback;
    } catch (_) {
        return fallback;
    }
}

export function writeJson(key, value) {
    if (value === null || value === undefined) {
        localStorage.removeItem(key);
    } else {
        localStorage.setItem(key, JSON.stringify(value));
    }
}

export function asArray(value) {
    return Array.isArray(value) ? value : [];
}

function normalizeStoredUser(user) {
    if (!user) return null;
    const { accessToken, sessionToken, devToken, ...safeUser } = user;
    return safeUser;
}

export const state = {
    user: normalizeStoredUser(readJson("achabitation.user", null)),
    trips: [],
    selectedTrip: readJson("achabitation.selectedTrip", null),
    persons: [],
    expenses: [],
    summary: null,
    auditLogs: [],
    invitations: [],
    profile: readJson("achabitation.profile", null),
    pendingClaimTripId: readJson("achabitation.pendingClaimTripId", null)
};
