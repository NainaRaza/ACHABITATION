// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.openPersonForm = function () {
    if (!ctx.ensureTripSelected()) return;
    ctx.resetPersonForm();
    ctx.activateTab("persons");
    ctx.showPanel("personFormPanel");
    setTimeout(() => $("personName")?.focus(), 0);
};

ctx.loadPersons = async function (render = true) {
    if (!state.selectedTrip?.id) return;
    state.persons = await api(`/trips/${state.selectedTrip.id}/persons`);
    if (render) {
        ctx.renderPersons();
        ctx.renderExpensePersonOptions();
    }
};

ctx.resetPersonForm = function () {
    $("personForm").reset();
    $("personId").value = "";
    $("personFormTitle").textContent = "Ajouter une personne";
    $("personWeightMode").value = "LIVING_REST";
    $("personLivingRest").value = "1000";
    $("personActive").checked = true;
    ctx.renderPresencePeriodRows(ctx.defaultPresencePeriods());
    ctx.renderCustomConstraintCheckboxes([]);
    ctx.updatePersonFieldState();
};

ctx.updatePersonFieldState = function () {
    const averageMode = $("personWeightMode").value === "AVERAGE";
    const advanced = $("personAdvancedRav").checked;
    $("personLivingRest").disabled = averageMode || advanced;
    $("personAdvancedRav").disabled = averageMode;
    $("advancedRavBlock").classList.toggle("hidden", !advanced && !averageMode);
    ["netIncomeAfterTax", "rent", "credits", "fixedCharges", "transport", "insurance", "otherMandatoryExpenses", "menstrualProtection", "calculateRavBtn"].forEach(id => {
        $(id).disabled = averageMode || !advanced;
    });
    if (averageMode) {
        $("personAdvancedRav").checked = false;
        $("advancedRavBlock").classList.add("hidden");
    }
};

ctx.calculateAdvancedRav = function () {
    const income = ctx.asNumber("netIncomeAfterTax") || 0;
    const charges = ["rent", "credits", "fixedCharges", "transport", "insurance", "otherMandatoryExpenses", "menstrualProtection"]
        .map(id => ctx.asNumber(id) || 0)
        .reduce((a, b) => a + b, 0);
    const rav = Math.max(0, income - charges);
    ctx.setNumber("personLivingRest", rav.toFixed(2));
    showMessage(`Reste à vivre calculé : ${rav.toFixed(2)} €`);
};

ctx.selectedCustomConstraintsFromForm = function () {
    return Array.from(document.querySelectorAll("#customConstraintCheckboxes input:checked"))
        .map(input => canonicalConstraintName(input.value))
        .filter(Boolean);
};

ctx.renderCustomConstraintCheckboxes = function (selected = null) {
    const container = $("customConstraintCheckboxes");
    if (!container) return;
    const selectedSet = new Set((selected ?? ctx.selectedCustomConstraintsFromForm()).map(constraintKey));
    const names = ctx.selectedTripConstraintNames();
    if (!names.length) {
        container.innerHTML = `<p class="small">Aucune contrainte personnalisée pour ce voyage.</p>`;
        return;
    }
    container.innerHTML = names.map(name => `
        <label class="checkbox"><input type="checkbox" value="${ctx.escapeHtml(name)}" ${selectedSet.has(constraintKey(name)) ? "checked" : ""}> ${ctx.escapeHtml(name)}</label>
    `).join("");
};

ctx.addCustomConstraintFromForm = async function () {
    await ctx.addCustomConstraintFromInput($("newCustomConstraintName"), true);
};

ctx.parsePresencePeriods = function () {
    const periods = ctx.collectPresencePeriodInputs()
        .map((period, index) => ({ ...period, originalIndex: index + 1 }))
        .filter(period => period.startDate || period.endDate);

    if (!periods.length) throw new Error("Au moins une période de présence est obligatoire.");

    periods.forEach(period => {
        if (!period.startDate || !period.endDate) {
            throw new Error(`Période ${period.originalIndex} invalide : début et fin sont obligatoires.`);
        }
        if (ctx.isAfterIso(period.startDate, period.endDate)) {
            throw new Error(`Période ${period.originalIndex} invalide : le début est après la fin.`);
        }
        ctx.validateDateInsideSelectedTrip(period.startDate, `La période de présence ${period.originalIndex}`);
        ctx.validateDateInsideSelectedTrip(period.endDate, `La période de présence ${period.originalIndex}`);
    });

    const sorted = periods
        .map(period => ({ startDate: period.startDate, endDate: period.endDate }))
        .sort((a, b) => a.startDate.localeCompare(b.startDate));

    let previous = null;
    sorted.forEach(period => {
        if (previous && !ctx.isAfterIso(period.startDate, previous.endDate)) {
            throw new Error("Les périodes de présence d'une même personne ne doivent pas se chevaucher.");
        }
        previous = period;
    });

    return sorted;
};

ctx.personPayload = function (includeActive) {
    const averageMode = $("personWeightMode").value === "AVERAGE";
    const advanced = $("personAdvancedRav").checked && !averageMode;
    const payload = {
        name: $("personName").value.trim(),
        livingRest: averageMode ? 0 : (advanced ? null : ctx.asNumber("personLivingRest")),
        weightMode: $("personWeightMode").value,
        advancedLivingRest: advanced,
        netIncomeAfterTax: advanced ? ctx.asNumber("netIncomeAfterTax") : null,
        rent: advanced ? ctx.asNumber("rent") : null,
        credits: advanced ? ctx.asNumber("credits") : null,
        fixedCharges: advanced ? ctx.asNumber("fixedCharges") : null,
        transport: advanced ? ctx.asNumber("transport") : null,
        insurance: advanced ? ctx.asNumber("insurance") : null,
        otherMandatoryExpenses: advanced ? ctx.asNumber("otherMandatoryExpenses") : null,
        menstrualProtection: advanced ? ctx.asNumber("menstrualProtection") : null,
        vegetarian: $("personVegetarian").checked,
        noAlcohol: $("personNoAlcohol").checked,
        livingRestPublic: state.profile?.livingRestPublic || false,
        customConstraints: ctx.selectedCustomConstraintsFromForm(),
        presencePeriods: ctx.parsePresencePeriods()
    };
    if (includeActive) payload.active = $("personActive").checked;
    return payload;
};

ctx.savePerson = async function (event) {
    event.preventDefault();
    if (!ctx.ensureTripSelected()) return;
    try {
        const id = $("personId").value;
        if (id) {
            await api(`/trips/${state.selectedTrip.id}/persons/${id}`, {
                method: "PUT",
                body: JSON.stringify(ctx.personPayload(true))
            });
            showMessage("Personne modifiée.");
        } else {
            await api(`/trips/${state.selectedTrip.id}/persons`, {
                method: "POST",
                body: JSON.stringify(ctx.personPayload(false))
            });
            showMessage("Personne ajoutée.");
        }
        ctx.resetPersonForm();
        ctx.hidePanel("personFormPanel");
        await ctx.loadTripData();
    } catch (error) {
        showMessage(`Enregistrement refusé : ${error.message}`, "error");
    }
};

ctx.editPerson = function (id) {
    const person = state.persons.find(p => p.id === id);
    if (!person) return;
    $("personId").value = person.id;
    $("personFormTitle").textContent = `Modifier ${person.name}`;
    $("personName").value = person.name;
    $("personWeightMode").value = person.weightMode || "LIVING_REST";
    ctx.setNumber("personLivingRest", person.livingRestHidden ? "" : (person.livingRest ?? 0));
    $("personAdvancedRav").checked = !!person.advancedLivingRest;
    ctx.setNumber("netIncomeAfterTax", person.livingRestHidden ? "" : (person.netIncomeAfterTax ?? 0));
    ctx.setNumber("rent", person.livingRestHidden ? "" : (person.rent ?? 0));
    ctx.setNumber("credits", person.livingRestHidden ? "" : (person.credits ?? 0));
    ctx.setNumber("fixedCharges", person.livingRestHidden ? "" : (person.fixedCharges ?? 0));
    ctx.setNumber("transport", person.livingRestHidden ? "" : (person.transport ?? 0));
    ctx.setNumber("insurance", person.livingRestHidden ? "" : (person.insurance ?? 0));
    ctx.setNumber("otherMandatoryExpenses", person.livingRestHidden ? "" : (person.otherMandatoryExpenses ?? 0));
    ctx.setNumber("menstrualProtection", person.livingRestHidden ? "" : (person.menstrualProtection ?? 0));
    ctx.renderCustomConstraintCheckboxes(person.customConstraints || []);
    $("personVegetarian").checked = !!person.vegetarian;
    $("personNoAlcohol").checked = !!person.noAlcohol;
    $("personActive").checked = !!person.active;
    ctx.renderPresencePeriodRows(person.presencePeriods || ctx.defaultPresencePeriods());
    ctx.updatePersonFieldState();
    ctx.activateTab("persons");
    ctx.showPanel("personFormPanel");
    $("personFormPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
};

ctx.disablePerson = async function (id) {
    if (!confirm("Désactiver cette personne ? Elle restera conservée pour l’historique.")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/persons/${id}`, { method: "DELETE" });
        await ctx.loadTripData();
        showMessage("Personne désactivée.");
    } catch (error) {
        showMessage(`Désactivation refusée : ${error.message}`, "error");
    }
};

ctx.linkPersonToCurrentUser = async function (id) {
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant de lier un guest à ton compte.", "error");
        return;
    }
    const person = state.persons.find(p => p.id === id);
    if (!person) return;
    if (!state.profile) await ctx.loadProfile(false);

    let applyProfileToGuest = false;
    const profileLivingRest = Number(state.profile?.livingRest ?? 0);
    if (profileLivingRest > 0) {
        applyProfileToGuest = confirm(
            `Lier ${person.name} à ton compte ?\n\n` +
            `OK : lier et remplacer ses données RAV / contraintes par ton profil actuel.\n` +
            `Annuler : lier sans modifier ses données actuelles.`
        );
    } else if (!confirm(`Lier ${person.name} à ton compte sans modifier ses données actuelles ? Ton profil RAV est à zéro, donc il ne sera pas appliqué.`)) {
        return;
    }

    try {
        await api(`/trips/${state.selectedTrip.id}/join`, {
            method: "POST",
            body: JSON.stringify({ guestPersonId: id, applyProfileToGuest })
        });
        state.pendingClaimTripId = null;
        writeJson("achabitation.pendingClaimTripId", null);
        ctx.hidePanel("postJoinClaimPanel");
        await ctx.loadProfile(false);
        await ctx.loadTripData();
        showMessage(applyProfileToGuest ? "Guest lié à ton compte et profil appliqué." : "Guest lié à ton compte sans modifier ses données.");
    } catch (error) {
        showMessage(`Liaison refusée : ${error.message}`, "error");
    }
};

ctx.renderPersons = function () {
    const tbody = $("personsTable");
    if (!state.selectedTrip) {
        tbody.innerHTML = `<tr><td colspan="5">Sélectionne un voyage.</td></tr>`;
        return;
    }
    if (!state.persons.length) {
        tbody.innerHTML = `<tr><td colspan="5">Aucune personne.</td></tr>`;
        return;
    }
    tbody.innerHTML = state.persons.map(person => {
        const weight = person.livingRestHidden
            ? "Masqué"
            : person.weightMode === "AVERAGE" ? "Moyenne" : money(person.livingRest, selectedCurrency());
        const profileBadge = person.guest ? "Guest" : `Compte lié${person.linkedUserEmail ? ` · ${person.linkedUserEmail}` : ""}`;
        const options = [profileBadge, person.vegetarian ? "Végétarien" : null, person.noAlcohol ? "Sans alcool" : null, ...(person.customConstraints || []), !person.active ? "Désactivé" : null]
            .filter(Boolean).map(o => `<span class="badge">${ctx.escapeHtml(o)}</span>`).join("") || "—";
        const periods = (person.presencePeriods || []).map(p => `${dateFr(p.startDate)} → ${dateFr(p.endDate)}`).join("<br>");
        const linkButton = state.user?.accessToken && person.guest
            ? `<button class="secondary small-button" type="button" data-link-person="${person.id}">Lier à mon compte</button>`
            : "";
        return `
            <tr>
                <td><strong>${ctx.escapeHtml(person.name)}</strong></td>
                <td>${weight}<br><span class="small">${person.livingRestHidden ? "RAV privé" : person.advancedLivingRest ? "RAV avancé" : person.weightMode === "AVERAGE" ? "RAV non utilisé" : "RAV simple"}</span></td>
                <td>${options}</td>
                <td>${periods || "—"}</td>
                <td><div class="row-actions">
                    <button class="secondary small-button" type="button" data-edit-person="${person.id}">Modifier</button>
                    ${linkButton}
                    <button class="danger small-button" type="button" data-disable-person="${person.id}">Désactiver</button>
                </div></td>
            </tr>
        `;
    }).join("");
    tbody.querySelectorAll("[data-edit-person]").forEach(b => b.addEventListener("click", () => ctx.editPerson(b.dataset.editPerson)));
    tbody.querySelectorAll("[data-link-person]").forEach(b => b.addEventListener("click", () => ctx.linkPersonToCurrentUser(b.dataset.linkPerson)));
    tbody.querySelectorAll("[data-disable-person]").forEach(b => b.addEventListener("click", () => ctx.disablePerson(b.dataset.disablePerson)));
};
}
