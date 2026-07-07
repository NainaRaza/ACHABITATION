// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.openExpenseForm = function () {
    if (!ctx.ensureTripSelected()) return;
    ctx.resetExpenseForm();
    ctx.activateTab("expenses");
    ctx.showPanel("expenseFormPanel");
    setTimeout(() => $("expenseTitle")?.focus(), 0);
};

ctx.loadExpenses = async function (render = true) {
    if (!state.selectedTrip?.id) return;
    state.expenses = await api(`/trips/${state.selectedTrip.id}/expenses`);
    if (render) ctx.renderExpenses();
};

ctx.resetExpenseForm = function () {
    $("expenseForm").reset();
    $("expenseId").value = "";
    $("expenseFormTitle").textContent = "Ajouter une dépense";
    const defaultDate = state.selectedTrip?.startDate && ctx.isBeforeIso(ctx.todayIso(), state.selectedTrip.startDate)
        ? state.selectedTrip.startDate
        : state.selectedTrip?.endDate && ctx.isAfterIso(ctx.todayIso(), state.selectedTrip.endDate)
            ? state.selectedTrip.endDate
            : ctx.todayIso();
    $("expenseDate").value = defaultDate;
    $("expenseType").value = "NORMAL";
    $("expenseCurrency").value = selectedCurrency();
    $("expenseExchangeRate").value = "1";
    ctx.renderExpensePersonOptions();
    ctx.renderExpenseCustomAmountRows({});
    ctx.updateExpenseFieldState();
    ctx.renderExpenseAllocationPreview();
};

ctx.renderExpensePersonOptions = function () {
    const payer = $("expensePayer");
    const activePersons = state.persons.filter(p => p.active);
    payer.innerHTML = activePersons.map(p => `<option value="${p.id}">${ctx.escapeHtml(p.name)}</option>`).join("");
    ctx.renderManualParticipants();
    ctx.renderExpenseCustomAmountRows();
    ctx.renderExpenseAllocationPreview();
};

ctx.collectExpenseCustomAmounts = function () {
    const result = {};
    document.querySelectorAll(".expense-custom-amount").forEach(input => {
        const name = input.dataset.constraintName;
        const amount = Number(input.value || 0);
        if (name && amount > 0) {
            result[name] = amount;
        }
    });
    return result;
};

ctx.renderExpenseCustomAmountRows = function (values = null) {
    const container = $("expenseCustomAmountRows");
    if (!container) return;
    const currentValues = values ?? ctx.collectExpenseCustomAmounts();
    const names = ctx.selectedTripConstraintNames();
    if (!names.length) {
        container.innerHTML = `<p class="small">Aucune contrainte personnalisée disponible. Ajoute-la dans les contraintes du voyage.</p>`;
        return;
    }
    container.innerHTML = names.map(name => {
        const matchingKey = Object.keys(currentValues || {}).find(key => constraintKey(key) === constraintKey(name));
        const value = matchingKey ? currentValues[matchingKey] : 0;
        return `
            <label>${ctx.escapeHtml(name)}
                <input class="expense-custom-amount" data-constraint-name="${ctx.escapeHtml(name)}" type="number" min="0" step="0.01" value="${value || 0}">
            </label>
        `;
    }).join("");
};

ctx.renderManualParticipants = function () {
    const block = $("manualParticipantsBlock");
    const checked = $("expenseAdvancedMode").checked;
    block.classList.toggle("hidden", !checked);
    const selectedIds = new Set(Array.from(document.querySelectorAll("#manualParticipants input:checked")).map(i => i.value));
    const activePersons = state.persons.filter(p => p.active);
    $("manualParticipants").innerHTML = activePersons.map(p => `
        <label class="checkbox"><input type="checkbox" value="${p.id}" ${selectedIds.has(p.id) ? "checked" : ""}> ${ctx.escapeHtml(p.name)}</label>
    `).join("") || `<p class="small">Ajoute d’abord des personnes actives.</p>`;
};

ctx.updateExpenseFieldState = function () {
    ctx.renderManualParticipants();
    ctx.renderExpenseAllocationPreview();
};


ctx.expenseDraftForPreview = function () {
    return {
        title: $("expenseTitle")?.value?.trim() || "Dépense en cours",
        date: $("expenseDate")?.value || ctx.todayIso(),
        totalAmount: ctx.asNumber("expenseTotal") || 0,
        meatAmount: ctx.asNumber("expenseMeat") || 0,
        alcoholAmount: ctx.asNumber("expenseAlcohol") || 0,
        customConstraintAmounts: ctx.collectExpenseCustomAmounts(),
        type: $("expenseType")?.value || "NORMAL",
        advancedMode: !!$("expenseAdvancedMode")?.checked,
        manualParticipantIds: Array.from(document.querySelectorAll("#manualParticipants input:checked")).map(input => input.value),
        currency: $("expenseCurrency")?.value?.trim()?.toUpperCase() || selectedCurrency(),
        exchangeRateToTripCurrency: ctx.asNumber("expenseExchangeRate") || 1
    };
};

ctx.renderExpenseAllocationPreview = function () {
    const container = $("expenseAllocationPreview");
    if (!container) return;
    if (!state.selectedTrip) {
        container.innerHTML = `<p class="small">Sélectionne un voyage pour prévisualiser la répartition.</p>`;
        return;
    }

    const expense = ctx.expenseDraftForPreview();
    const eligible = state.persons.filter(ctx.canParticipateInAllocation);
    if (!eligible.length) {
        container.innerHTML = `<p class="small">Ajoute au moins une personne active avec un RAV exploitable, ou un mode moyenne, pour prévisualiser la répartition.</p>`;
        return;
    }

    const baseParticipants = expense.advancedMode
        ? eligible.filter(person => new Set(expense.manualParticipantIds || []).has(person.id))
        : expense.type === "GLOBAL"
            ? eligible
            : eligible.filter(person => ctx.isPersonPresentOn(person, expense.date));

    const total = Number(expense.totalAmount || 0);
    const meat = Number(expense.meatAmount || 0);
    const alcohol = Number(expense.alcoholAmount || 0);
    const customEntries = Object.entries(expense.customConstraintAmounts || {}).filter(([, amount]) => Number(amount || 0) > 0);
    const customTotal = customEntries.reduce((sum, [, amount]) => sum + Number(amount || 0), 0);
    const specialisedTotal = meat + alcohol + customTotal;
    const general = Math.max(0, total - specialisedTotal);

    const chips = persons => persons.length
        ? persons.map(person => `<span class="person-chip">${ctx.escapeHtml(person.name)}</span>`).join("")
        : `<span class="badge warning">Aucune</span>`;

    const rows = [];
    if (general > 0) {
        rows.push(`<li><strong>Part commune ${money(general, expense.currency)}</strong><br>${chips(baseParticipants)}</li>`);
    }
    if (meat > 0) {
        const included = baseParticipants.filter(person => !person.vegetarian);
        const excluded = baseParticipants.filter(person => person.vegetarian);
        rows.push(`<li><strong>Part viande ${money(meat, expense.currency)}</strong><br>${chips(included)}${excluded.length ? `<br><span class="small">Exclu·es : ${excluded.map(person => ctx.escapeHtml(person.name)).join(", ")}</span>` : ""}</li>`);
    }
    if (alcohol > 0) {
        const included = baseParticipants.filter(person => !person.noAlcohol);
        const excluded = baseParticipants.filter(person => person.noAlcohol);
        rows.push(`<li><strong>Part alcool ${money(alcohol, expense.currency)}</strong><br>${chips(included)}${excluded.length ? `<br><span class="small">Exclu·es : ${excluded.map(person => ctx.escapeHtml(person.name)).join(", ")}</span>` : ""}</li>`);
    }
    customEntries.forEach(([constraintName, amount]) => {
        const included = baseParticipants.filter(person => !ctx.personHasCustomConstraint(person, constraintName));
        const excluded = baseParticipants.filter(person => ctx.personHasCustomConstraint(person, constraintName));
        rows.push(`<li><strong>${ctx.escapeHtml(constraintName)} ${money(amount, expense.currency)}</strong><br>${chips(included)}${excluded.length ? `<br><span class="small">Exclu·es : ${excluded.map(person => ctx.escapeHtml(person.name)).join(", ")}</span>` : ""}</li>`);
    });

    const warnings = [];
    if (expense.advancedMode && !baseParticipants.length) warnings.push("Mode avancé actif : aucune personne cochée.");
    if (!expense.advancedMode && expense.type === "NORMAL" && !baseParticipants.length) warnings.push("Aucune personne présente à cette date avec un RAV exploitable.");
    if (specialisedTotal > total) warnings.push("Les montants viande, alcool et contraintes dépassent le total saisi.");

    container.innerHTML = `
        <div class="field-header">
            <span class="field-label">Prévisualisation de la répartition</span>
        </div>
        <p class="small">${expense.type === "GLOBAL" ? "Mutualisée voyage : toutes les personnes éligibles sont prises en compte, puis les exclusions sont appliquées." : "Datée : seules les personnes présentes à la date choisie sont prises en compte, puis les exclusions sont appliquées."}</p>
        ${warnings.map(message => `<p class="small warning-text">${ctx.escapeHtml(message)}</p>`).join("")}
        ${rows.length ? `<ul class="allocation-preview-list">${rows.join("")}</ul>` : `<p class="small">Renseigne un total ou des montants par contrainte pour afficher le détail.</p>`}
    `;
};

ctx.expensePayload = function () {
    const manualParticipantIds = Array.from(document.querySelectorAll("#manualParticipants input:checked")).map(input => input.value);
    return {
        title: $("expenseTitle").value.trim(),
        date: $("expenseDate").value,
        payerPersonId: $("expensePayer").value,
        totalAmount: ctx.asNumber("expenseTotal"),
        meatAmount: ctx.asNumber("expenseMeat") || 0,
        alcoholAmount: ctx.asNumber("expenseAlcohol") || 0,
        customConstraintAmounts: ctx.collectExpenseCustomAmounts(),
        type: $("expenseType").value,
        advancedMode: $("expenseAdvancedMode").checked,
        manualParticipantIds,
        currency: $("expenseCurrency").value.trim().toUpperCase() || selectedCurrency(),
        exchangeRateToTripCurrency: ctx.asNumber("expenseExchangeRate") || 1
    };
};

ctx.saveExpense = async function (event) {
    event.preventDefault();
    if (!ctx.ensureTripSelected()) return;
    if (!state.persons.filter(p => p.active).length) {
        showMessage("Ajoute au moins une personne active avant une dépense.", "error");
        return;
    }
    try {
        const payload = ctx.expensePayload();
        ctx.validateDateInsideSelectedTrip(payload.date, "La date de dépense");
        const id = $("expenseId").value;
        if (id) {
            await api(`/trips/${state.selectedTrip.id}/expenses/${id}`, {
                method: "PUT",
                body: JSON.stringify(payload)
            });
            showMessage("Dépense modifiée.");
        } else {
            await api(`/trips/${state.selectedTrip.id}/expenses`, {
                method: "POST",
                body: JSON.stringify(payload)
            });
            showMessage("Dépense ajoutée.");
        }
        ctx.resetExpenseForm();
        ctx.hidePanel("expenseFormPanel");
        await ctx.loadTripData();
    } catch (error) {
        showMessage(`Enregistrement refusé : ${error.message}`, "error");
    }
};

ctx.editExpense = function (id) {
    const expense = state.expenses.find(e => e.id === id);
    if (!expense) return;
    $("expenseId").value = expense.id;
    $("expenseFormTitle").textContent = `Modifier ${expense.title}`;
    $("expenseTitle").value = expense.title;
    $("expenseDate").value = expense.date;
    $("expensePayer").value = expense.payerPersonId;
    ctx.setNumber("expenseTotal", expense.totalAmount);
    ctx.setNumber("expenseMeat", expense.meatAmount || 0);
    ctx.setNumber("expenseAlcohol", expense.alcoholAmount || 0);
    ctx.renderExpenseCustomAmountRows(expense.customConstraintAmounts || {});
    $("expenseType").value = expense.type || "NORMAL";
    $("expenseAdvancedMode").checked = !!expense.advancedMode;
    $("expenseCurrency").value = expense.currency || selectedCurrency();
    ctx.setNumber("expenseExchangeRate", expense.exchangeRateToTripCurrency || 1);
    ctx.renderManualParticipants();
    const manualIds = new Set(expense.manualParticipantIds || []);
    document.querySelectorAll("#manualParticipants input").forEach(input => input.checked = manualIds.has(input.value));
    ctx.updateExpenseFieldState();
    ctx.activateTab("expenses");
    ctx.showPanel("expenseFormPanel");
    $("expenseFormPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
};

ctx.deleteExpense = async function (id) {
    if (!confirm("Supprimer cette dépense ?")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/expenses/${id}`, { method: "DELETE" });
        await ctx.loadTripData();
        showMessage("Dépense supprimée.");
    } catch (error) {
        showMessage(`Suppression refusée : ${error.message}`, "error");
    }
};

ctx.canParticipateInAllocation = function (person) {
    if (!person?.active) return false;
    if (person.livingRestHidden) return true;
    if (person.weightMode === "AVERAGE") return true;
    return Number(person.livingRest ?? 0) > 0;
};

ctx.isPersonPresentOn = function (person, date) {
    if (!person || !date) return false;
    return (person.presencePeriods || []).some(period =>
        period.startDate && period.endDate
        && !ctx.isAfterIso(period.startDate, date)
        && !ctx.isBeforeIso(period.endDate, date)
    );
};

ctx.uniquePersons = function (persons) {
    const seen = new Set();
    return persons.filter(person => {
        if (!person?.id || seen.has(person.id)) return false;
        seen.add(person.id);
        return true;
    });
};

ctx.concernedPersonsForExpense = function (expense) {
    const eligible = state.persons.filter(ctx.canParticipateInAllocation);

    if (expense.advancedMode) {
        const selectedIds = new Set(expense.manualParticipantIds || []);
        return eligible.filter(person => selectedIds.has(person.id));
    }

    const baseParticipants = expense.type === "GLOBAL"
        ? eligible
        : eligible.filter(person => ctx.isPersonPresentOn(person, expense.date));
    const meat = Number(expense.meatAmount || 0);
    const alcohol = Number(expense.alcoholAmount || 0);
    const total = Number(expense.totalAmount || 0);
    const customAmounts = expense.customConstraintAmounts || {};
    const customTotal = Object.values(customAmounts).map(Number).reduce((a, b) => a + (b || 0), 0);
    const general = Math.max(0, total - meat - alcohol - customTotal);

    const concerned = [];
    if (general > 0) concerned.push(...baseParticipants);
    if (meat > 0) concerned.push(...baseParticipants.filter(person => !person.vegetarian));
    if (alcohol > 0) concerned.push(...baseParticipants.filter(person => !person.noAlcohol));
    Object.entries(customAmounts).forEach(([constraintName, amount]) => {
        if (Number(amount || 0) > 0) {
            concerned.push(...baseParticipants.filter(person => !ctx.personHasCustomConstraint(person, constraintName)));
        }
    });

    return ctx.uniquePersons(concerned);
};

ctx.renderConcernedPersons = function (expense) {
    const persons = ctx.concernedPersonsForExpense(expense);
    if (!persons.length) {
        return `<span class="badge warning">Aucune</span><br><span class="small">Dépense invalide ou données incomplètes</span>`;
    }

    const label = expense.advancedMode
        ? "Mode avancé"
        : expense.type === "GLOBAL"
            ? "Mutualisée voyage · contraintes appliquées"
            : "Datée · présence / végé / alcool / contraintes";

    const names = persons.map(person => person.name).join(", ");
    const chips = persons.map(person => `<span class="person-chip">${ctx.escapeHtml(person.name)}</span>`).join("");
    return `<div class="participant-list" title="${ctx.escapeHtml(names)}">${chips}</div><span class="small">${persons.length} personne${persons.length > 1 ? "s" : ""} · ${label}</span>`;
};

ctx.expenseTypeLabel = function (type) {
    return type === "GLOBAL" ? "Mutualisée voyage" : "Datée";
};

ctx.renderExpenseDetails = function (expense) {
    const details = [
        `Viande ${money(expense.meatAmount || 0, expense.currency)}`,
        `Alcool ${money(expense.alcoholAmount || 0, expense.currency)}`,
        ...Object.entries(expense.customConstraintAmounts || {})
            .filter(([, amount]) => Number(amount || 0) > 0)
            .map(([name, amount]) => `${ctx.escapeHtml(name)} ${money(amount, expense.currency)}`)
    ];
    return details.join(" · ");
};

ctx.renderExpenses = function () {
    const tbody = $("expensesTable");
    if (!state.selectedTrip) {
        tbody.innerHTML = `<tr><td colspan="7">Sélectionne un voyage.</td></tr>`;
        return;
    }
    if (!state.expenses.length) {
        tbody.innerHTML = `<tr><td colspan="7">Aucune dépense.</td></tr>`;
        return;
    }
    tbody.innerHTML = state.expenses.map(expense => `
        <tr>
            <td><strong>${ctx.escapeHtml(expense.title)}</strong><br><span class="small">${ctx.renderExpenseDetails(expense)}</span></td>
            <td>${dateFr(expense.date)}</td>
            <td>${ctx.escapeHtml(expense.payerName || "—")}</td>
            <td>${money(expense.totalAmount, expense.currency)}<br><span class="small">Taux ${expense.exchangeRateToTripCurrency || 1}</span></td>
            <td><span class="badge">${ctx.expenseTypeLabel(expense.type)}</span>${expense.advancedMode ? `<span class="badge">Avancé</span>` : ""}</td>
            <td>${ctx.renderConcernedPersons(expense)}</td>
            <td><div class="row-actions">
                <button class="secondary small-button" type="button" data-edit-expense="${expense.id}">Modifier</button>
                <button class="danger small-button" type="button" data-delete-expense="${expense.id}">Supprimer</button>
            </div></td>
        </tr>
    `).join("");
    tbody.querySelectorAll("[data-edit-expense]").forEach(b => b.addEventListener("click", () => ctx.editExpense(b.dataset.editExpense)));
    tbody.querySelectorAll("[data-delete-expense]").forEach(b => b.addEventListener("click", () => ctx.deleteExpense(b.dataset.deleteExpense)));
};
}
