// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.loadSummary = async function (render = true) {
    if (!state.selectedTrip?.id) return;
    try {
        state.summary = await api(`/trips/${state.selectedTrip.id}/summary`);
        if (render) ctx.renderSummary();
    } catch (error) {
        showMessage(`Résumé indisponible : ${error.message}`, "error");
    }
};

ctx.renderSummary = function () {
    const balances = state.summary?.balances || [];
    const settlements = state.summary?.settlements || [];
    const tbody = $("balancesTable");
    if (!state.selectedTrip) {
        tbody.innerHTML = `<tr><td colspan="4">Sélectionne un voyage.</td></tr>`;
        $("settlementsList").innerHTML = `<div class="list-item">Aucun voyage sélectionné.</div>`;
        return;
    }
    if (!balances.length) {
        tbody.innerHTML = `<tr><td colspan="4">Aucun solde calculable.</td></tr>`;
    } else {
        tbody.innerHTML = balances.map(balance => {
            const cls = Number(balance.balance) > 0 ? "amount-positive" : Number(balance.balance) < 0 ? "amount-negative" : "amount-neutral";
            return `<tr>
                <td>${ctx.escapeHtml(balance.personName)}</td>
                <td>${money(balance.totalPaid)}</td>
                <td>${money(balance.totalOwed)}</td>
                <td class="${cls}">${money(balance.balance)}</td>
            </tr>`;
        }).join("");
    }
    $("settlementsList").innerHTML = settlements.length
        ? settlements.map(s => `<div class="list-item"><strong>${ctx.escapeHtml(s.fromPersonName)} rembourse ${money(s.amount)} à ${ctx.escapeHtml(s.toPersonName)}</strong></div>`).join("")
        : `<div class="list-item"><strong>Aucun remboursement nécessaire</strong><span class="small">Les soldes sont déjà équilibrés ou aucune dépense n’a été saisie.</span></div>`;
};
}
