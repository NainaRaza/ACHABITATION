// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.selectedTripConstraintNames = function () {
    if (!state.selectedTrip?.id) return [];
    const byKey = new Map();
    (state.selectedTrip.customConstraints || []).forEach(name => {
        const displayName = canonicalConstraintName(name);
        const key = constraintKey(displayName);
        if (key && !byKey.has(key)) byKey.set(key, displayName);
    });
    return Array.from(byKey.values()).sort((a, b) => a.localeCompare(b, "fr"));
};

ctx.addConstraintToSelectedTrip = async function (name) {
    if (!ctx.ensureTripSelected()) return null;
    const displayName = canonicalConstraintName(name);
    const key = constraintKey(displayName);
    if (!key) return null;
    const current = ctx.selectedTripConstraintNames();
    if (current.some(existing => constraintKey(existing) === key)) return displayName;
    const updated = [...current, displayName];
    state.selectedTrip = await api(`/trips/${state.selectedTrip.id}/constraints`, {
        method: "PUT",
        body: JSON.stringify({ customConstraints: updated })
    });
    writeJson("achabitation.selectedTrip", state.selectedTrip);
    await ctx.loadTrips();
    const refreshed = state.trips.find(t => t.id === state.selectedTrip.id);
    if (refreshed) state.selectedTrip = refreshed;
    if (state.user?.accessToken) await ctx.loadProfile(false);
    return displayName;
};

ctx.personHasCustomConstraint = function (person, name) {
    const key = constraintKey(name);
    return (person?.customConstraints || []).some(value => constraintKey(value) === key);
};

ctx.renderTripConstraints = function () {
    const list = $("tripConstraintList");
    if (!list) return;
    if (!state.selectedTrip) {
        list.innerHTML = `<span class="small">Aucun voyage sélectionné.</span>`;
        return;
    }
    const names = ctx.selectedTripConstraintNames();
    list.innerHTML = names.length
        ? names.map(name => `<span class="badge">${ctx.escapeHtml(name)}</span>`).join("")
        : `<span class="small">Aucune contrainte déclarée.</span>`;
};

ctx.addTripConstraintFromSidebar = async function () {
    const input = $("newTripConstraintName");
    await ctx.addCustomConstraintFromInput(input, false);
};

ctx.addCustomConstraintFromInput = async function (input, checkInPersonForm = true) {
    try {
        const displayName = await ctx.addConstraintToSelectedTrip(input.value);
        if (!displayName) {
            showMessage("Nom de contrainte invalide.", "error");
            return null;
        }
        input.value = "";
        const selected = ctx.selectedCustomConstraintsFromForm();
        if (checkInPersonForm && !selected.some(value => constraintKey(value) === constraintKey(displayName))) {
            selected.push(displayName);
        }
        ctx.renderTripConstraints();
        ctx.renderCustomConstraintCheckboxes(selected);
        ctx.renderProfileConstraintCheckboxes(state.profile?.customConstraints || []);
        ctx.renderExpenseCustomAmountRows();
        showMessage("Contrainte ajoutée au voyage.");
        return displayName;
    } catch (error) {
        showMessage(`Contrainte refusée : ${error.message}`, "error");
        return null;
    }
};
}
