// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.loadInvitations = async function (render = true) {
    if (!state.selectedTrip?.id || !state.user?.userId) {
        state.invitations = [];
        if (render) ctx.renderInvitations();
        return;
    }
    try {
        state.invitations = await api(`/trips/${state.selectedTrip.id}/invitations`);
    } catch (error) {
        state.invitations = [];
        // Les participant·es non admin n'ont pas accès aux invitations. Ce n'est pas une erreur bloquante d'affichage.
    }
    if (render) ctx.renderInvitations();
};

ctx.createInvitation = async function () {
    if (!ctx.ensureTripSelected()) return;
    try {
        const roleToGrant = $("inviteRole")?.value || "PARTICIPANT";
        await api(`/trips/${state.selectedTrip.id}/invitations`, {
            method: "POST",
            body: JSON.stringify({ roleToGrant, expiresInDays: 7 })
        });
        await ctx.loadInvitations();
        showMessage("Invitation créée.");
    } catch (error) {
        showMessage(`Invitation refusée : ${error.message}`, "error");
    }
};

ctx.revokeInvitation = async function (invitationId) {
    if (!ctx.ensureTripSelected()) return;
    if (!confirm("Révoquer cette invitation ?")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/invitations/${invitationId}`, { method: "DELETE" });
        await ctx.loadInvitations();
        showMessage("Invitation révoquée.");
    } catch (error) {
        showMessage(`Révocation refusée : ${error.message}`, "error");
    }
};

ctx.renderInvitations = function () {
    const container = $("invitationList");
    if (!container) return;
    if (!state.selectedTrip) {
        container.innerHTML = `<p class="small">Aucun voyage sélectionné.</p>`;
        return;
    }
    if (!state.invitations.length) {
        container.innerHTML = `<p class="small">Aucune invitation visible. Seuls owner/admin peuvent les gérer.</p>`;
        return;
    }
    container.innerHTML = state.invitations.map(invitation => `
        <div class="list-item">
            <strong>${ctx.escapeHtml(invitation.code)}</strong>
            <span class="small">Rôle : ${ctx.escapeHtml(invitation.roleToGrant)} · Expire : ${dateFr((invitation.expiresAt || "").slice(0,10))} · ${invitation.revoked ? "Révoquée" : invitation.usable ? "Active" : "Expirée"}</span>
            <div class="row-actions"><button type="button" class="danger small-button" data-revoke-invite="${ctx.escapeHtml(invitation.id)}">Révoquer</button></div>
        </div>
    `).join("");
    container.querySelectorAll("[data-revoke-invite]").forEach(button => {
        button.addEventListener("click", () => ctx.revokeInvitation(button.dataset.revokeInvite));
    });
};
}
