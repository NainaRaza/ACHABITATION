// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.asNumber = function (id) {
    const value = $(id).value;
    if (value === "" || value === null || value === undefined) return null;
    return Number(value);
};

ctx.setNumber = function (id, value) {
    $(id).value = value === null || value === undefined ? "" : value;
};

ctx.ensureTripSelected = function () {
    if (!state.selectedTrip?.id) {
        showMessage("Aucun voyage sélectionné.", "error");
        return false;
    }
    return true;
};

ctx.todayIso = function () {
    return new Date().toISOString().slice(0, 10);
};

ctx.isBeforeIso = function (a, b) {
    return !!a && !!b && a < b;
};

ctx.isAfterIso = function (a, b) {
    return !!a && !!b && a > b;
};

ctx.validateTripPayload = function (payload) {
    if (!payload.startDate || !payload.endDate) {
        throw new Error("Les dates de début et de fin du voyage sont obligatoires.");
    }
    if (ctx.isAfterIso(payload.startDate, payload.endDate)) {
        throw new Error("La date de début du voyage doit être antérieure ou égale à sa date de fin.");
    }
};

ctx.validateDateInsideSelectedTrip = function (date, label) {
    if (!state.selectedTrip || !date) return;
    if (ctx.isBeforeIso(date, state.selectedTrip.startDate)) {
        throw new Error(`${label} est antérieure au début du voyage.`);
    }
    if (ctx.isAfterIso(date, state.selectedTrip.endDate)) {
        throw new Error(`${label} est postérieure à la fin du voyage.`);
    }
};

ctx.defaultPresencePeriods = function () {
    return [{
        startDate: state.selectedTrip?.startDate || "",
        endDate: state.selectedTrip?.endDate || ""
    }];
};

ctx.collectPresencePeriodInputs = function () {
    return Array.from(document.querySelectorAll(".presence-period-row")).map(row => ({
        startDate: row.querySelector(".presence-period-start")?.value || "",
        endDate: row.querySelector(".presence-period-end")?.value || ""
    }));
};

ctx.renderPresencePeriodRows = function (periods = null) {
    const container = $("presencePeriodsRows");
    if (!container) return;
    const normalized = (periods && periods.length ? periods : ctx.defaultPresencePeriods()).map(period => ({
        startDate: period.startDate || "",
        endDate: period.endDate || ""
    }));
    const min = state.selectedTrip?.startDate || "";
    const max = state.selectedTrip?.endDate || "";

    container.innerHTML = normalized.map((period, index) => `
        <div class="presence-period-row" data-period-index="${index}">
            <label>Début de période
                <input class="presence-period-start" type="date" value="${ctx.escapeHtml(period.startDate)}" ${min ? `min="${min}"` : ""} ${max ? `max="${max}"` : ""} required>
            </label>
            <label>Fin de période
                <input class="presence-period-end" type="date" value="${ctx.escapeHtml(period.endDate)}" ${min ? `min="${min}"` : ""} ${max ? `max="${max}"` : ""} required>
            </label>
            <button class="danger small-button remove-presence-period" type="button" ${normalized.length <= 1 ? "disabled" : ""} title="Supprimer cette période">−</button>
        </div>
    `).join("");
    ctx.updatePresencePeriodDateBounds();
};

ctx.updatePresencePeriodDateBounds = function () {
    const min = state.selectedTrip?.startDate || "";
    const max = state.selectedTrip?.endDate || "";
    document.querySelectorAll(".presence-period-start, .presence-period-end").forEach(input => {
        input.min = min;
        input.max = max;
    });
    document.querySelectorAll(".presence-period-row").forEach(row => {
        const start = row.querySelector(".presence-period-start");
        const end = row.querySelector(".presence-period-end");
        if (start && end) {
            end.min = start.value || min;
            start.max = end.value || max;
        }
    });
};

ctx.addPresencePeriodRow = function () {
    const periods = ctx.collectPresencePeriodInputs();
    periods.push({ startDate: "", endDate: "" });
    ctx.renderPresencePeriodRows(periods);
};

ctx.removePresencePeriodRow = function (button) {
    const row = button.closest(".presence-period-row");
    if (!row) return;
    const periods = ctx.collectPresencePeriodInputs();
    const index = Number(row.dataset.periodIndex);
    if (periods.length <= 1) return;
    periods.splice(index, 1);
    ctx.renderPresencePeriodRows(periods);
};

ctx.showPanel = function (id) {
    const panel = $(id);
    if (panel) panel.classList.remove("hidden");
};

ctx.hidePanel = function (id) {
    const panel = $(id);
    if (panel) panel.classList.add("hidden");
};

ctx.escapeHtml = function (value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
};
}
