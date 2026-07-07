// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, sanitizeAuthUser, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

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
        state.user = sanitizeAuthUser(await api("/auth/login", { method: "POST", body: JSON.stringify(payload) }));
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
        const response = await api("/auth/register", { method: "POST", body: JSON.stringify(payload) });
        $("registerPassword").value = "";
        if (response?.accessToken) {
            state.user = sanitizeAuthUser(response);
            await ctx.afterAuthentication(response.note || "Compte créé et connecté.");
        } else {
            state.user = null;
            ctx.clearSessionState();
            ctx.hydrateUserUi();
            ctx.renderAll();
            showMessage(response?.note || "Compte créé. Vérifie ton email avant connexion.");
        }
    } catch (error) {
        showMessage(`Création du compte refusée : ${error.message}`, "error");
    }
};

ctx.requestPasswordReset = async function () {
    const email = $("resetRequestEmail")?.value?.trim();
    if (!email) {
        showMessage("Renseigne l’email du compte à réinitialiser.", "error");
        return;
    }
    try {
        const response = await api("/auth/password/reset-request", {
            method: "POST",
            body: JSON.stringify({ email })
        });
        showMessage(response?.note || "Un email de réinitialisation va être envoyé si le compte existe.");
    } catch (error) {
        showMessage(`Demande de reset refusée : ${error.message}`, "error");
    }
};

ctx.confirmPasswordReset = async function () {
    const token = $("resetPasswordToken")?.value?.trim();
    const newPassword = $("resetPasswordNew")?.value || "";
    if (!token || newPassword.length < 8) {
        showMessage("Renseigne le jeton et un nouveau mot de passe d’au moins 8 caractères.", "error");
        return;
    }
    try {
        state.user = sanitizeAuthUser(await api("/auth/password/reset", {
            method: "POST",
            body: JSON.stringify({ token, newPassword })
        }));
        $("resetPasswordToken").value = "";
        $("resetPasswordNew").value = "";
        ctx.cleanAccountActionQueryParams();
        await ctx.afterAuthentication("Mot de passe réinitialisé et compte connecté.");
    } catch (error) {
        showMessage(`Réinitialisation refusée : ${error.message}`, "error");
    }
};

ctx.confirmEmailVerification = async function (token) {
    if (!token) {
        showMessage("Jeton de vérification email absent.", "error");
        return;
    }
    try {
        state.user = sanitizeAuthUser(await api("/auth/email/verify", {
            method: "POST",
            body: JSON.stringify({ token })
        }));
        ctx.cleanAccountActionQueryParams();
        await ctx.afterAuthentication("Email vérifié et compte connecté.");
    } catch (error) {
        showMessage(`Vérification email refusée : ${error.message}`, "error");
    }
};

ctx.requestEmailVerification = async function () {
    const email = state.user?.email || $("loginIdentifier")?.value?.trim() || $("resetRequestEmail")?.value?.trim();
    if (!email) {
        showMessage("Renseigne l’email du compte à vérifier.", "error");
        return;
    }
    try {
        const response = await api("/auth/email/verification-request", {
            method: "POST",
            body: JSON.stringify({ email })
        });
        showMessage(response?.note || "Email de vérification demandé.");
    } catch (error) {
        showMessage(`Demande de vérification refusée : ${error.message}`, "error");
    }
};

ctx.cleanAccountActionQueryParams = function () {
    const url = new URL(window.location.href);
    url.searchParams.delete("resetToken");
    url.searchParams.delete("verifyEmailToken");
    window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
};

ctx.handleAccountActionTokens = async function () {
    const params = new URLSearchParams(window.location.search);
    const resetToken = params.get("resetToken");
    const verifyEmailToken = params.get("verifyEmailToken");
    if (resetToken && $("resetPasswordToken")) {
        $("resetPasswordToken").value = resetToken;
        $("resetPasswordNew")?.focus();
        ctx.openProfilePanel();
        showMessage("Lien de réinitialisation détecté. Renseigne ton nouveau mot de passe.");
    }
    if (verifyEmailToken) {
        ctx.openProfilePanel();
        await ctx.confirmEmailVerification(verifyEmailToken);
    }
};

ctx.updateAccount = async function (event) {
    event.preventDefault();
    if (!state.user?.userId) {
        showMessage("Connecte-toi avant de modifier ton compte.", "error");
        return;
    }
    const payload = {
        email: $("accountEmail").value.trim(),
        displayName: $("accountDisplayName").value.trim()
    };
    try {
        const updatedAccount = await api("/auth/account", { method: "PUT", body: JSON.stringify(payload) });
        state.user = sanitizeAuthUser({ ...state.user, ...updatedAccount });
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

ctx.changePassword = async function (event) {
    event.preventDefault();
    if (!state.user?.userId) {
        showMessage("Connecte-toi avant de changer ton mot de passe.", "error");
        return;
    }
    const payload = {
        currentPassword: $("currentPassword").value,
        newPassword: $("newPassword").value
    };
    if (!payload.currentPassword || !payload.newPassword || payload.newPassword.length < 8) {
        showMessage("Renseigne le mot de passe actuel et un nouveau mot de passe d’au moins 8 caractères.", "error");
        return;
    }
    try {
        const updated = await api("/auth/password", { method: "PUT", body: JSON.stringify(payload) });
        state.user = sanitizeAuthUser({ ...state.user, ...updated });
        writeJson("achabitation.user", state.user);
        $("currentPassword").value = "";
        $("newPassword").value = "";
        ctx.hidePanel("passwordChangeForm");
        showMessage("Mot de passe modifié.");
    } catch (error) {
        showMessage(`Changement de mot de passe refusé : ${error.message}`, "error");
    }
};

ctx.exportAccountData = async function () {
    if (!state.user?.userId) {
        showMessage("Connecte-toi avant d’exporter tes données.", "error");
        return;
    }
    try {
        const exportData = await api("/auth/export");
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: "application/json" });
        const link = document.createElement("a");
        link.href = window.URL.createObjectURL(blob);
        link.download = `achabitation-export-${new Date().toISOString().slice(0, 10)}.json`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(link.href);
        showMessage("Export de compte généré.");
    } catch (error) {
        showMessage(`Export de compte refusé : ${error.message}`, "error");
    }
};

ctx.deleteAccount = async function () {
    if (!state.user?.userId) {
        showMessage("Connecte-toi avant de supprimer ton compte.", "error");
        return;
    }
    if (!confirm("Supprimer ton compte ? Cette action anonymise le compte et déconnecte la session.")) return;
    try {
        await api("/auth/account", { method: "DELETE" });
        ctx.clearSessionState();
        ctx.hydrateUserUi();
        ctx.renderAll();
        ctx.activateTab("dashboard");
        ctx.openProfilePanel();
        showMessage("Compte supprimé et session locale nettoyée.");
    } catch (error) {
        showMessage(`Suppression de compte refusée : ${error.message}`, "error");
    }
};

ctx.logoutUser = async function () {
    try {
        if (state.user?.userId) {
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
