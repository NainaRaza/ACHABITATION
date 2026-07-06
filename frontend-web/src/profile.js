// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.openProfilePanel = async function () {
    document.querySelectorAll(".tab").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-panel").forEach(panel => panel.classList.remove("active"));
    $("profilePanel")?.classList.add("active");
    if (state.user?.accessToken) {
        await ctx.loadProfile();
    } else {
        ctx.renderProfile();
    }
    $("profilePanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
};

ctx.checkBackend = async function () {
    try {
        await api("/health");
        $("backendStatus").textContent = "Backend connecté";
        $("backendStatus").style.background = "rgba(30, 111, 67, .35)";
    } catch (error) {
        $("backendStatus").textContent = "Backend indisponible";
        $("backendStatus").style.background = "rgba(169, 33, 33, .35)";
        showMessage(`Backend indisponible : ${error.message}`, "error");
    }
};

ctx.hydrateUserUi = function () {
    const connected = !!state.user?.accessToken;
    $("accountLoggedOutPanel")?.classList.toggle("hidden", connected);
    $("accountLoggedInPanel")?.classList.toggle("hidden", !connected);
    $("profileCard")?.classList.toggle("hidden", !connected);
    $("profileApplyPanel")?.classList.add("hidden");

    if (connected) {
        if ($("topUserName")) $("topUserName").textContent = state.user.displayName || state.user.email;
        if ($("accountEmailDisplay")) $("accountEmailDisplay").textContent = state.user.email || "—";
        if ($("accountDisplayNameDisplay")) $("accountDisplayNameDisplay").textContent = state.user.displayName || "—";
        if ($("accountEmail")) $("accountEmail").value = state.user.email || "";
        if ($("accountDisplayName")) $("accountDisplayName").value = state.user.displayName || "";
    } else {
        if ($("topUserName")) $("topUserName").textContent = "Non connecté";
        ["loginIdentifier", "loginPassword", "registerEmail", "registerDisplayName", "registerPassword"].forEach(id => {
            const el = $(id);
            if (el) el.value = "";
        });
    }
};

ctx.loadProfile = async function (render = true) {
    if (!state.user?.accessToken) return;
    try {
        state.profile = await api("/auth/profile");
        writeJson("achabitation.profile", state.profile);
        if (render) ctx.renderProfile();
    } catch (error) {
        showMessage(`Profil indisponible : ${error.message}`, "error");
    }
};

ctx.renderProfile = function () {
    const disabled = !state.user?.accessToken;
    ["profileWeightMode", "profileLivingRest", "profileAdvancedRav", "profileNetIncomeAfterTax", "profileRent", "profileCredits", "profileFixedCharges", "profileTransport", "profileInsurance", "profileOtherMandatoryExpenses", "profileMenstrualProtection", "profileVegetarian", "profileNoAlcohol", "profileLivingRestPublic"].forEach(id => {
        const el = $(id);
        if (el) el.disabled = disabled;
    });
    if (!state.profile) {
        ctx.renderProfileConstraintCheckboxes([]);
        ctx.renderProfileLinkedTrips();
        ctx.updateProfileFieldState();
        return;
    }
    $("profileWeightMode").value = state.profile.weightMode || "LIVING_REST";
    ctx.setNumber("profileLivingRest", state.profile.livingRest ?? 1000);
    $("profileAdvancedRav").checked = !!state.profile.advancedLivingRest;
    ctx.setNumber("profileNetIncomeAfterTax", state.profile.netIncomeAfterTax ?? 0);
    ctx.setNumber("profileRent", state.profile.rent ?? 0);
    ctx.setNumber("profileCredits", state.profile.credits ?? 0);
    ctx.setNumber("profileFixedCharges", state.profile.fixedCharges ?? 0);
    ctx.setNumber("profileTransport", state.profile.transport ?? 0);
    ctx.setNumber("profileInsurance", state.profile.insurance ?? 0);
    ctx.setNumber("profileOtherMandatoryExpenses", state.profile.otherMandatoryExpenses ?? 0);
    ctx.setNumber("profileMenstrualProtection", state.profile.menstrualProtection ?? 0);
    $("profileVegetarian").checked = !!state.profile.vegetarian;
    $("profileNoAlcohol").checked = !!state.profile.noAlcohol;
    $("profileLivingRestPublic").checked = !!state.profile.livingRestPublic;
    ctx.renderProfileConstraintCheckboxes(state.profile.customConstraints || []);
    ctx.renderProfileLinkedTrips();
    ctx.updateProfileFieldState();
};

ctx.updateProfileFieldState = function () {
    const averageMode = $("profileWeightMode")?.value === "AVERAGE";
    const advanced = !!$("profileAdvancedRav")?.checked && !averageMode;
    if ($("profileLivingRest")) $("profileLivingRest").disabled = !state.user?.accessToken || averageMode || advanced;
    if ($("profileAdvancedRav")) $("profileAdvancedRav").disabled = !state.user?.accessToken || averageMode;
    if ($("profileAdvancedBlock")) $("profileAdvancedBlock").classList.toggle("hidden", !advanced);
    ["profileNetIncomeAfterTax", "profileRent", "profileCredits", "profileFixedCharges", "profileTransport", "profileInsurance", "profileOtherMandatoryExpenses", "profileMenstrualProtection"].forEach(id => {
        if ($(id)) $(id).disabled = !state.user?.accessToken || !advanced;
    });
    if (averageMode && $("profileAdvancedRav")) $("profileAdvancedRav").checked = false;
};

ctx.renderProfileConstraintCheckboxes = function (selected = []) {
    const container = $("profileConstraintCheckboxes");
    if (!container) return;
    const byKey = new Map();
    [...(state.profile?.knownCustomConstraints || []), ...ctx.selectedTripConstraintNames()].forEach(name => {
        const displayName = canonicalConstraintName(name);
        const key = constraintKey(displayName);
        if (key && !byKey.has(key)) byKey.set(key, displayName);
    });
    const names = Array.from(byKey.values()).sort((a, b) => a.localeCompare(b, "fr"));
    const selectedSet = new Set((selected || []).map(constraintKey));
    if (!names.length) {
        container.innerHTML = `<p class="small">Aucune contrainte déclarée dans le voyage sélectionné.</p>`;
        return;
    }
    container.innerHTML = names.map(name => `
        <label class="checkbox"><input type="checkbox" value="${ctx.escapeHtml(name)}" ${selectedSet.has(constraintKey(name)) ? "checked" : ""}> ${ctx.escapeHtml(name)}</label>
    `).join("");
};

ctx.selectedProfileConstraintsFromForm = function () {
    return Array.from(document.querySelectorAll("#profileConstraintCheckboxes input:checked"))
        .map(input => canonicalConstraintName(input.value))
        .filter(Boolean);
};

ctx.renderProfileLinkedTrips = function () {
    const container = $("profileLinkedTripsList");
    if (!container) return;
    const linkedPersons = state.profile?.linkedPersons || [];
    if (!state.user?.accessToken) {
        container.innerHTML = `<p class="small">Connecte-toi pour voir tes voyages liés.</p>`;
        return;
    }
    if (!linkedPersons.length) {
        container.innerHTML = `<p class="small">Ton compte n’est encore lié à aucun guest.</p>`;
        return;
    }
    container.innerHTML = linkedPersons.map(item => `
        <label class="checkbox">
            <input type="checkbox" value="${ctx.escapeHtml(item.personId)}">
            <span>${ctx.escapeHtml(item.tripName)} · ${ctx.escapeHtml(item.personName)}</span>
        </label>
    `).join("");
};

ctx.selectedLinkedPersonIdsForProfileApply = function () {
    return Array.from(document.querySelectorAll("#profileLinkedTripsList input:checked"))
        .map(input => input.value)
        .filter(Boolean);
};

ctx.applyProfileToSelectedLinkedPersons = async function () {
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant d’appliquer ton profil.", "error");
        return;
    }
    const personIds = ctx.selectedLinkedPersonIdsForProfileApply();
    if (!personIds.length) {
        showMessage("Sélectionne au moins un voyage lié à mettre à jour.", "error");
        return;
    }
    try {
        state.profile = await api("/auth/profile/apply-to-linked-persons", {
            method: "POST",
            body: JSON.stringify({ personIds })
        });
        writeJson("achabitation.profile", state.profile);
        ctx.renderProfileLinkedTrips();
        if (state.selectedTrip?.id) await ctx.loadTripData();
        showMessage("Profil appliqué aux voyages sélectionnés.");
    } catch (error) {
        showMessage(`Application refusée : ${error.message}`, "error");
    }
};

ctx.profilePayload = function () {
    const averageMode = $("profileWeightMode").value === "AVERAGE";
    const advanced = $("profileAdvancedRav").checked && !averageMode;
    return {
        displayName: state.user?.displayName || state.profile?.displayName || "",
        livingRest: averageMode ? 0 : (advanced ? null : ctx.asNumber("profileLivingRest")),
        weightMode: $("profileWeightMode").value,
        advancedLivingRest: advanced,
        netIncomeAfterTax: advanced ? ctx.asNumber("profileNetIncomeAfterTax") : null,
        rent: advanced ? ctx.asNumber("profileRent") : null,
        credits: advanced ? ctx.asNumber("profileCredits") : null,
        fixedCharges: advanced ? ctx.asNumber("profileFixedCharges") : null,
        transport: advanced ? ctx.asNumber("profileTransport") : null,
        insurance: advanced ? ctx.asNumber("profileInsurance") : null,
        otherMandatoryExpenses: advanced ? ctx.asNumber("profileOtherMandatoryExpenses") : null,
        menstrualProtection: advanced ? ctx.asNumber("profileMenstrualProtection") : null,
        vegetarian: $("profileVegetarian").checked,
        noAlcohol: $("profileNoAlcohol").checked,
        livingRestPublic: $("profileLivingRestPublic").checked,
        customConstraints: ctx.selectedProfileConstraintsFromForm()
    };
};

ctx.saveProfile = async function (event) {
    event.preventDefault();
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant de modifier ton profil.", "error");
        return;
    }
    try {
        state.profile = await api("/auth/profile", { method: "PUT", body: JSON.stringify(ctx.profilePayload()) });
        state.user.displayName = state.profile.displayName;
        writeJson("achabitation.user", state.user);
        writeJson("achabitation.profile", state.profile);
        ctx.hydrateUserUi();
        ctx.renderProfile();
        ctx.showPanel("profileApplyPanel");
        showMessage("Profil enregistré. Il n’a pas été appliqué automatiquement aux voyages liés.");
    } catch (error) {
        showMessage(`Profil refusé : ${error.message}`, "error");
    }
};
}
