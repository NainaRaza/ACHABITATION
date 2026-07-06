// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.loadAuditLogs = async function (render = true) {
    if (!state.selectedTrip?.id) return;
    try {
        state.auditLogs = await api(`/trips/${state.selectedTrip.id}/audit-logs`);
        if (render) ctx.renderAuditLogs();
    } catch (error) {
        showMessage(`Historique indisponible : ${error.message}`, "error");
    }
};

ctx.renderAuditLogs = function () {
    const list = $("auditList");
    if (!state.selectedTrip) {
        list.innerHTML = `<div class="audit-item">Sélectionne un voyage.</div>`;
        return;
    }
    if (!state.auditLogs.length) {
        list.innerHTML = `<div class="audit-item">Aucune modification historisée.</div>`;
        return;
    }
    list.innerHTML = state.auditLogs.map(log => `
        <div class="audit-item">
            <strong>${ctx.escapeHtml(log.action || "ACTION")}</strong>
            <p class="small">${new Date(log.createdAt).toLocaleString("fr-FR")} · ${ctx.escapeHtml(log.entityType || "")}</p>
            <p>${ctx.escapeHtml(log.description || "")}</p>
        </div>
    `).join("");
};
}
