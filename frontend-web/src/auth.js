// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.clearSessionState = function () {
    state.user = null;
    state.profile = null;
    state.trips = [];
    state.selectedTrip = null;
    state.persons = [];
    state.expenses = [];
    state.summary = null;
    state.auditLogs = [];
    state.invitations = [];
    writeJson("achabitation.user", null);
    writeJson("achabitation.profile", null);
    writeJson("achabitation.selectedTrip", null);
};

ctx.afterAuthentication = async function (message) {
    writeJson("achabitation.user", state.user);
    ctx.hydrateUserUi();
    await ctx.loadProfile();
    await ctx.loadTrips();
    const selected = state.selectedTrip?.id ? state.trips.find(t => t.id === state.selectedTrip.id) : null;
    if (selected) {
        state.selectedTrip = selected;
        writeJson("achabitation.selectedTrip", selected);
        await ctx.loadTripData();
    } else if (state.trips.length > 0) {
        await ctx.selectTrip(state.trips[0].id);
    } else {
        state.selectedTrip = null;
        writeJson("achabitation.selectedTrip", null);
        ctx.renderAll();
    }
    showMessage(message);
};

ctx.loginUser = async function () {
    const payload = {
        email: $("loginIdentifier").value.trim(),
        password: $("loginPassword").value
    };

    if (!payload.email) {
        showMessage("Renseigne ton email ou ton nom affiché.", "error");
        return;
    }
    if (!payload.password) {
        showMessage("Renseigne ton mot de passe.", "error");
        return;
    }

    try {
        state.user = await api("/auth/login", { method: "POST", body: JSON.stringify(payload) });
        $("loginPassword").value = "";
        await ctx.afterAuthentication("Compte connecté.");
    } catch (error) {
        showMessage(`Connexion refusée : ${error.message}`, "error");
    }
};

ctx.createUser = async function () {
    const payload = {
        email: $("registerEmail").value.trim(),
        displayName: $("registerDisplayName").value.trim(),
        password: $("registerPassword").value
    };

    if (!payload.email || !payload.displayName) {
        showMessage("Renseigne un email et un nom affiché.", "error");
        return;
    }
    if (!payload.password || payload.password.length < 8) {
        showMessage("Le mot de passe doit contenir au moins 8 caractères.", "error");
        return;
    }

    try {
        state.user = await api("/auth/register", { method: "POST", body: JSON.stringify(payload) });
        $("registerPassword").value = "";
        await ctx.afterAuthentication("Compte créé et connecté.");
    } catch (error) {
        showMessage(`Création du compte refusée : ${error.message}`, "error");
    }
};

ctx.updateAccount = async function (event) {
    event.preventDefault();
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant de modifier ton compte.", "error");
        return;
    }
    const payload = {
        email: $("accountEmail").value.trim(),
        displayName: $("accountDisplayName").value.trim()
    };
    try {
        const updatedAccount = await api("/auth/account", { method: "PUT", body: JSON.stringify(payload) });
        state.user = { ...state.user, ...updatedAccount, accessToken: updatedAccount.accessToken || state.user.accessToken };
        writeJson("achabitation.user", state.user);
        ctx.hydrateUserUi();
        ctx.hidePanel("accountEditForm");
        if (state.profile) {
            state.profile.email = state.user.email;
            state.profile.displayName = state.user.displayName;
            writeJson("achabitation.profile", state.profile);
        }
        showMessage("Compte mis à jour.");
    } catch (error) {
        showMessage(`Modification du compte refusée : ${error.message}`, "error");
    }
};

ctx.logoutUser = async function () {
    try {
        if (state.user?.accessToken) {
            await api("/auth/logout", { method: "POST" });
        }
    } catch (_) {
        // Même si l'appel serveur échoue, on nettoie la session locale.
    }
    ctx.clearSessionState();
    ctx.hydrateUserUi();
    ctx.renderAll();
    ctx.activateTab("dashboard");
    ctx.openProfilePanel();
    showMessage("Compte déconnecté.");
};
}
