const API = "/api/v1";

const state = {
    user: readJson("achabitation.user", null),
    trips: [],
    selectedTrip: readJson("achabitation.selectedTrip", null),
    persons: [],
    expenses: [],
    summary: null,
    auditLogs: [],
    invitations: [],
    profile: readJson("achabitation.profile", null)
};

const $ = (id) => document.getElementById(id);

function readJson(key, fallback) {
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : fallback;
    } catch (_) {
        return fallback;
    }
}

function writeJson(key, value) {
    if (value === null || value === undefined) {
        localStorage.removeItem(key);
    } else {
        localStorage.setItem(key, JSON.stringify(value));
    }
}

function asArray(value) {
    return Array.isArray(value) ? value : [];
}

function money(value, currency = selectedCurrency()) {
    const number = Number(value ?? 0);
    return new Intl.NumberFormat("fr-FR", {
        style: "currency",
        currency: currency || "EUR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(number);
}

function dateFr(value) {
    if (!value) return "—";
    return new Date(`${value}T00:00:00`).toLocaleDateString("fr-FR");
}

function selectedCurrency() {
    return state.selectedTrip?.referenceCurrency || "EUR";
}

function canonicalConstraintName(value) {
    return String(value ?? "").trim().replace(/\s+/g, " ");
}

function constraintKey(value) {
    return canonicalConstraintName(value)
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

function selectedTripConstraintNames() {
    if (!state.selectedTrip?.id) return [];
    const byKey = new Map();
    (state.selectedTrip.customConstraints || []).forEach(name => {
        const displayName = canonicalConstraintName(name);
        const key = constraintKey(displayName);
        if (key && !byKey.has(key)) byKey.set(key, displayName);
    });
    return Array.from(byKey.values()).sort((a, b) => a.localeCompare(b, "fr"));
}

async function addConstraintToSelectedTrip(name) {
    if (!ensureTripSelected()) return null;
    const displayName = canonicalConstraintName(name);
    const key = constraintKey(displayName);
    if (!key) return null;
    const current = selectedTripConstraintNames();
    if (current.some(existing => constraintKey(existing) === key)) return displayName;
    const updated = [...current, displayName];
    state.selectedTrip = await api(`/trips/${state.selectedTrip.id}/constraints`, {
        method: "PUT",
        body: JSON.stringify({ customConstraints: updated })
    });
    writeJson("achabitation.selectedTrip", state.selectedTrip);
    await loadTrips();
    const refreshed = state.trips.find(t => t.id === state.selectedTrip.id);
    if (refreshed) state.selectedTrip = refreshed;
    if (state.user?.devToken) await loadProfile(false);
    return displayName;
}

function personHasCustomConstraint(person, name) {
    const key = constraintKey(name);
    return (person?.customConstraints || []).some(value => constraintKey(value) === key);
}

function asNumber(id) {
    const value = $(id).value;
    if (value === "" || value === null || value === undefined) return null;
    return Number(value);
}

function setNumber(id, value) {
    $(id).value = value === null || value === undefined ? "" : value;
}

async function api(path, options = {}) {
    const authHeaders = state.user?.devToken ? { "Authorization": `Bearer ${state.user.devToken}` } : {};
    const response = await fetch(`${API}${path}`, {
        headers: { "Content-Type": "application/json", ...authHeaders, ...(options.headers || {}) },
        ...options
    });

    if (!response.ok) {
        let details = `${response.status} ${response.statusText}`;
        try {
            const body = await response.json();
            if (body.details && Array.isArray(body.details)) {
                details = body.details.join("\n");
            } else if (body.message) {
                details = body.message;
            } else if (body.error) {
                details = `${body.error}${body.path ? ` - ${body.path}` : ""}`;
            } else {
                details = JSON.stringify(body);
            }
        } catch (_) {
            const text = await response.text().catch(() => "");
            if (text) details = text;
        }
        throw new Error(details);
    }

    if (response.status === 204) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
}

function showMessage(message, type = "success") {
    const box = $("messageBox");
    box.textContent = message;
    box.className = `message ${type}`;
    box.classList.remove("hidden");
    window.clearTimeout(showMessage._timer);
    showMessage._timer = window.setTimeout(() => box.classList.add("hidden"), type === "error" ? 9000 : 3500);
}

function ensureTripSelected() {
    if (!state.selectedTrip?.id) {
        showMessage("Aucun voyage sélectionné.", "error");
        return false;
    }
    return true;
}

function todayIso() {
    return new Date().toISOString().slice(0, 10);
}

function isBeforeIso(a, b) {
    return !!a && !!b && a < b;
}

function isAfterIso(a, b) {
    return !!a && !!b && a > b;
}

function validateTripPayload(payload) {
    if (!payload.startDate || !payload.endDate) {
        throw new Error("Les dates de début et de fin du voyage sont obligatoires.");
    }
    if (isAfterIso(payload.startDate, payload.endDate)) {
        throw new Error("La date de début du voyage doit être antérieure ou égale à sa date de fin.");
    }
}

function validateDateInsideSelectedTrip(date, label) {
    if (!state.selectedTrip || !date) return;
    if (isBeforeIso(date, state.selectedTrip.startDate)) {
        throw new Error(`${label} est antérieure au début du voyage.`);
    }
    if (isAfterIso(date, state.selectedTrip.endDate)) {
        throw new Error(`${label} est postérieure à la fin du voyage.`);
    }
}

function defaultPresencePeriods() {
    return [{
        startDate: state.selectedTrip?.startDate || "",
        endDate: state.selectedTrip?.endDate || ""
    }];
}

function collectPresencePeriodInputs() {
    return Array.from(document.querySelectorAll(".presence-period-row")).map(row => ({
        startDate: row.querySelector(".presence-period-start")?.value || "",
        endDate: row.querySelector(".presence-period-end")?.value || ""
    }));
}

function renderPresencePeriodRows(periods = null) {
    const container = $("presencePeriodsRows");
    if (!container) return;
    const normalized = (periods && periods.length ? periods : defaultPresencePeriods()).map(period => ({
        startDate: period.startDate || "",
        endDate: period.endDate || ""
    }));
    const min = state.selectedTrip?.startDate || "";
    const max = state.selectedTrip?.endDate || "";

    container.innerHTML = normalized.map((period, index) => `
        <div class="presence-period-row" data-period-index="${index}">
            <label>Début de période
                <input class="presence-period-start" type="date" value="${escapeHtml(period.startDate)}" ${min ? `min="${min}"` : ""} ${max ? `max="${max}"` : ""} required>
            </label>
            <label>Fin de période
                <input class="presence-period-end" type="date" value="${escapeHtml(period.endDate)}" ${min ? `min="${min}"` : ""} ${max ? `max="${max}"` : ""} required>
            </label>
            <button class="danger small-button remove-presence-period" type="button" ${normalized.length <= 1 ? "disabled" : ""} title="Supprimer cette période">−</button>
        </div>
    `).join("");
    updatePresencePeriodDateBounds();
}

function updatePresencePeriodDateBounds() {
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
}

function addPresencePeriodRow() {
    const periods = collectPresencePeriodInputs();
    periods.push({ startDate: "", endDate: "" });
    renderPresencePeriodRows(periods);
}

function removePresencePeriodRow(button) {
    const row = button.closest(".presence-period-row");
    if (!row) return;
    const periods = collectPresencePeriodInputs();
    const index = Number(row.dataset.periodIndex);
    if (periods.length <= 1) return;
    periods.splice(index, 1);
    renderPresencePeriodRows(periods);
}

async function init() {
    bindEvents();
    hydrateUserUi();
    await checkBackend();
    if (state.user?.devToken) {
        await loadProfile(false);
        await loadTrips();
        if (state.selectedTrip?.id && state.trips.some(t => t.id === state.selectedTrip.id)) {
            state.selectedTrip = state.trips.find(t => t.id === state.selectedTrip.id);
            await loadTripData();
        } else if (state.trips.length > 0) {
            selectTrip(state.trips[0].id);
        } else {
            renderAll();
        }
    } else {
        renderAll();
    }
}

function bindEvents() {
    $("loginUserBtn")?.addEventListener("click", loginUser);
    $("createUserBtn")?.addEventListener("click", createUser);
    $("logoutUserBtn")?.addEventListener("click", logoutUser);
    $("showAccountEditBtn")?.addEventListener("click", () => showPanel("accountEditForm"));
    $("cancelAccountEditBtn")?.addEventListener("click", () => {
        hydrateUserUi();
        hidePanel("accountEditForm");
    });
    $("accountEditForm")?.addEventListener("submit", updateAccount);
    ["loginIdentifier", "loginPassword"].forEach(id => $(id)?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            loginUser();
        }
    }));
    ["registerEmail", "registerDisplayName", "registerPassword"].forEach(id => $(id)?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            createUser();
        }
    }));
    $("topProfileBtn").addEventListener("click", openProfilePanel);
    $("closeProfilePanelBtn").addEventListener("click", () => activateTab("dashboard"));
    $("hideProfileApplyPanelBtn").addEventListener("click", () => hidePanel("profileApplyPanel"));
    $("applyProfileToLinkedBtn").addEventListener("click", applyProfileToSelectedLinkedPersons);

    $("showTripFormBtn").addEventListener("click", () => showPanel("tripFormPanel"));
    $("closeTripFormBtn").addEventListener("click", () => hidePanel("tripFormPanel"));
    $("tripForm").addEventListener("submit", createTrip);
    $("joinTripByCodeBtn")?.addEventListener("click", joinTripByInvitationCode);
    $("joinInvitationCode")?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            joinTripByInvitationCode();
        }
    });

    $("profileForm").addEventListener("submit", saveProfile);
    $("profileWeightMode").addEventListener("change", updateProfileFieldState);
    $("profileAdvancedRav").addEventListener("change", updateProfileFieldState);
    $("addTripConstraintBtn").addEventListener("click", addTripConstraintFromSidebar);
    $("newTripConstraintName").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            addTripConstraintFromSidebar();
        }
    });
    $("refreshBtn").addEventListener("click", refreshCurrent);

    document.querySelectorAll(".tab").forEach(button => {
        button.addEventListener("click", () => activateTab(button.dataset.tab));
    });

    $("quickAddExpenseBtn").addEventListener("click", () => openExpenseForm());
    $("quickAddPersonBtn").addEventListener("click", () => openPersonForm());
    $("dashboardAddExpenseBtn").addEventListener("click", () => openExpenseForm());
    $("dashboardAddPersonBtn").addEventListener("click", () => openPersonForm());
    $("dashboardSummaryBtn").addEventListener("click", () => activateTab("summary"));

    $("showPersonFormBtn").addEventListener("click", () => openPersonForm());
    $("closePersonFormBtn").addEventListener("click", () => hidePanel("personFormPanel"));
    $("personForm").addEventListener("submit", savePerson);
    $("resetPersonBtn").addEventListener("click", event => {
        event.preventDefault();
        resetPersonForm();
        showPanel("personFormPanel");
    });
    $("personWeightMode").addEventListener("change", updatePersonFieldState);
    $("personAdvancedRav").addEventListener("change", updatePersonFieldState);
    $("calculateRavBtn").addEventListener("click", calculateAdvancedRav);
    $("addCustomConstraintBtn").addEventListener("click", addCustomConstraintFromForm);
    $("newCustomConstraintName").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            addCustomConstraintFromForm();
        }
    });
    $("addPresencePeriodBtn").addEventListener("click", addPresencePeriodRow);
    $("presencePeriodsRows").addEventListener("click", event => {
        if (event.target.classList.contains("remove-presence-period")) removePresencePeriodRow(event.target);
    });
    $("presencePeriodsRows").addEventListener("change", updatePresencePeriodDateBounds);

    $("showExpenseFormBtn").addEventListener("click", () => openExpenseForm());
    $("closeExpenseFormBtn").addEventListener("click", () => hidePanel("expenseFormPanel"));
    $("expenseForm").addEventListener("submit", saveExpense);
    $("resetExpenseBtn").addEventListener("click", event => {
        event.preventDefault();
        resetExpenseForm();
        showPanel("expenseFormPanel");
    });
    $("expenseAdvancedMode").addEventListener("change", renderManualParticipants);
    $("expenseType").addEventListener("change", updateExpenseFieldState);

    $("reloadSummaryBtn").addEventListener("click", loadSummary);
    $("exportExpensesCsvBtn")?.addEventListener("click", () => downloadExport("expenses.csv"));
    $("exportSummaryCsvBtn")?.addEventListener("click", () => downloadExport("summary.csv"));
    $("reloadAuditBtn").addEventListener("click", loadAuditLogs);
    $("createInviteBtn")?.addEventListener("click", createInvitation);

    const defaultDate = state.selectedTrip?.startDate && isBeforeIso(todayIso(), state.selectedTrip.startDate)
        ? state.selectedTrip.startDate
        : state.selectedTrip?.endDate && isAfterIso(todayIso(), state.selectedTrip.endDate)
            ? state.selectedTrip.endDate
            : todayIso();
    $("expenseDate").value = defaultDate;
}

function showPanel(id) {
    const panel = $(id);
    if (panel) panel.classList.remove("hidden");
}

function hidePanel(id) {
    const panel = $(id);
    if (panel) panel.classList.add("hidden");
}

async function openProfilePanel() {
    document.querySelectorAll(".tab").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-panel").forEach(panel => panel.classList.remove("active"));
    $("profilePanel")?.classList.add("active");
    if (state.user?.devToken) {
        await loadProfile();
    } else {
        renderProfile();
    }
    $("profilePanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function openPersonForm() {
    if (!ensureTripSelected()) return;
    resetPersonForm();
    activateTab("persons");
    showPanel("personFormPanel");
    setTimeout(() => $("personName")?.focus(), 0);
}

function openExpenseForm() {
    if (!ensureTripSelected()) return;
    resetExpenseForm();
    activateTab("expenses");
    showPanel("expenseFormPanel");
    setTimeout(() => $("expenseTitle")?.focus(), 0);
}

function activateTab(tab) {
    document.querySelectorAll(".tab").forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
    document.querySelectorAll(".tab-panel").forEach(panel => panel.classList.remove("active"));
    $(`${tab}Tab`).classList.add("active");
    if (tab === "summary") loadSummary();
    if (tab === "audit") loadAuditLogs();
}

async function checkBackend() {
    try {
        await api("/health");
        $("backendStatus").textContent = "Backend connecté";
        $("backendStatus").style.background = "rgba(30, 111, 67, .35)";
    } catch (error) {
        $("backendStatus").textContent = "Backend indisponible";
        $("backendStatus").style.background = "rgba(169, 33, 33, .35)";
        showMessage(`Backend indisponible : ${error.message}`, "error");
    }
}

function hydrateUserUi() {
    const connected = !!state.user?.devToken;
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
}

async function loadProfile(render = true) {
    if (!state.user?.devToken) return;
    try {
        state.profile = await api("/auth/profile");
        writeJson("achabitation.profile", state.profile);
        if (render) renderProfile();
    } catch (error) {
        showMessage(`Profil indisponible : ${error.message}`, "error");
    }
}

function renderProfile() {
    const disabled = !state.user?.devToken;
    ["profileWeightMode", "profileLivingRest", "profileAdvancedRav", "profileNetIncomeAfterTax", "profileRent", "profileCredits", "profileFixedCharges", "profileTransport", "profileInsurance", "profileOtherMandatoryExpenses", "profileMenstrualProtection", "profileVegetarian", "profileNoAlcohol", "profileLivingRestPublic"].forEach(id => {
        const el = $(id);
        if (el) el.disabled = disabled;
    });
    if (!state.profile) {
        renderProfileConstraintCheckboxes([]);
        renderProfileLinkedTrips();
        updateProfileFieldState();
        return;
    }
    $("profileWeightMode").value = state.profile.weightMode || "LIVING_REST";
    setNumber("profileLivingRest", state.profile.livingRest ?? 1000);
    $("profileAdvancedRav").checked = !!state.profile.advancedLivingRest;
    setNumber("profileNetIncomeAfterTax", state.profile.netIncomeAfterTax ?? 0);
    setNumber("profileRent", state.profile.rent ?? 0);
    setNumber("profileCredits", state.profile.credits ?? 0);
    setNumber("profileFixedCharges", state.profile.fixedCharges ?? 0);
    setNumber("profileTransport", state.profile.transport ?? 0);
    setNumber("profileInsurance", state.profile.insurance ?? 0);
    setNumber("profileOtherMandatoryExpenses", state.profile.otherMandatoryExpenses ?? 0);
    setNumber("profileMenstrualProtection", state.profile.menstrualProtection ?? 0);
    $("profileVegetarian").checked = !!state.profile.vegetarian;
    $("profileNoAlcohol").checked = !!state.profile.noAlcohol;
    $("profileLivingRestPublic").checked = !!state.profile.livingRestPublic;
    renderProfileConstraintCheckboxes(state.profile.customConstraints || []);
    renderProfileLinkedTrips();
    updateProfileFieldState();
}

function updateProfileFieldState() {
    const averageMode = $("profileWeightMode")?.value === "AVERAGE";
    const advanced = !!$("profileAdvancedRav")?.checked && !averageMode;
    if ($("profileLivingRest")) $("profileLivingRest").disabled = !state.user?.devToken || averageMode || advanced;
    if ($("profileAdvancedRav")) $("profileAdvancedRav").disabled = !state.user?.devToken || averageMode;
    if ($("profileAdvancedBlock")) $("profileAdvancedBlock").classList.toggle("hidden", !advanced);
    ["profileNetIncomeAfterTax", "profileRent", "profileCredits", "profileFixedCharges", "profileTransport", "profileInsurance", "profileOtherMandatoryExpenses", "profileMenstrualProtection"].forEach(id => {
        if ($(id)) $(id).disabled = !state.user?.devToken || !advanced;
    });
    if (averageMode && $("profileAdvancedRav")) $("profileAdvancedRav").checked = false;
}

function renderProfileConstraintCheckboxes(selected = []) {
    const container = $("profileConstraintCheckboxes");
    if (!container) return;
    const byKey = new Map();
    [...(state.profile?.knownCustomConstraints || []), ...selectedTripConstraintNames()].forEach(name => {
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
        <label class="checkbox"><input type="checkbox" value="${escapeHtml(name)}" ${selectedSet.has(constraintKey(name)) ? "checked" : ""}> ${escapeHtml(name)}</label>
    `).join("");
}

function selectedProfileConstraintsFromForm() {
    return Array.from(document.querySelectorAll("#profileConstraintCheckboxes input:checked"))
        .map(input => canonicalConstraintName(input.value))
        .filter(Boolean);
}

function renderProfileLinkedTrips() {
    const container = $("profileLinkedTripsList");
    if (!container) return;
    const linkedPersons = state.profile?.linkedPersons || [];
    if (!state.user?.devToken) {
        container.innerHTML = `<p class="small">Connecte-toi pour voir tes voyages liés.</p>`;
        return;
    }
    if (!linkedPersons.length) {
        container.innerHTML = `<p class="small">Ton compte n’est encore lié à aucun guest.</p>`;
        return;
    }
    container.innerHTML = linkedPersons.map(item => `
        <label class="checkbox">
            <input type="checkbox" value="${escapeHtml(item.personId)}">
            <span>${escapeHtml(item.tripName)} · ${escapeHtml(item.personName)}</span>
        </label>
    `).join("");
}

function selectedLinkedPersonIdsForProfileApply() {
    return Array.from(document.querySelectorAll("#profileLinkedTripsList input:checked"))
        .map(input => input.value)
        .filter(Boolean);
}

async function applyProfileToSelectedLinkedPersons() {
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant d’appliquer ton profil.", "error");
        return;
    }
    const personIds = selectedLinkedPersonIdsForProfileApply();
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
        renderProfileLinkedTrips();
        if (state.selectedTrip?.id) await loadTripData();
        showMessage("Profil appliqué aux voyages sélectionnés.");
    } catch (error) {
        showMessage(`Application refusée : ${error.message}`, "error");
    }
}

function profilePayload() {
    const averageMode = $("profileWeightMode").value === "AVERAGE";
    const advanced = $("profileAdvancedRav").checked && !averageMode;
    return {
        displayName: state.user?.displayName || state.profile?.displayName || "",
        livingRest: averageMode ? 0 : (advanced ? null : asNumber("profileLivingRest")),
        weightMode: $("profileWeightMode").value,
        advancedLivingRest: advanced,
        netIncomeAfterTax: advanced ? asNumber("profileNetIncomeAfterTax") : null,
        rent: advanced ? asNumber("profileRent") : null,
        credits: advanced ? asNumber("profileCredits") : null,
        fixedCharges: advanced ? asNumber("profileFixedCharges") : null,
        transport: advanced ? asNumber("profileTransport") : null,
        insurance: advanced ? asNumber("profileInsurance") : null,
        otherMandatoryExpenses: advanced ? asNumber("profileOtherMandatoryExpenses") : null,
        menstrualProtection: advanced ? asNumber("profileMenstrualProtection") : null,
        vegetarian: $("profileVegetarian").checked,
        noAlcohol: $("profileNoAlcohol").checked,
        livingRestPublic: $("profileLivingRestPublic").checked,
        customConstraints: selectedProfileConstraintsFromForm()
    };
}

async function saveProfile(event) {
    event.preventDefault();
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant de modifier ton profil.", "error");
        return;
    }
    try {
        state.profile = await api("/auth/profile", { method: "PUT", body: JSON.stringify(profilePayload()) });
        state.user.displayName = state.profile.displayName;
        writeJson("achabitation.user", state.user);
        writeJson("achabitation.profile", state.profile);
        hydrateUserUi();
        renderProfile();
        showPanel("profileApplyPanel");
        showMessage("Profil enregistré. Il n’a pas été appliqué automatiquement aux voyages liés.");
    } catch (error) {
        showMessage(`Profil refusé : ${error.message}`, "error");
    }
}

function renderTripConstraints() {
    const list = $("tripConstraintList");
    if (!list) return;
    if (!state.selectedTrip) {
        list.innerHTML = `<span class="small">Aucun voyage sélectionné.</span>`;
        return;
    }
    const names = selectedTripConstraintNames();
    list.innerHTML = names.length
        ? names.map(name => `<span class="badge">${escapeHtml(name)}</span>`).join("")
        : `<span class="small">Aucune contrainte déclarée.</span>`;
}

async function addTripConstraintFromSidebar() {
    const input = $("newTripConstraintName");
    await addCustomConstraintFromInput(input, false);
}

async function addCustomConstraintFromInput(input, checkInPersonForm = true) {
    try {
        const displayName = await addConstraintToSelectedTrip(input.value);
        if (!displayName) {
            showMessage("Nom de contrainte invalide.", "error");
            return null;
        }
        input.value = "";
        const selected = selectedCustomConstraintsFromForm();
        if (checkInPersonForm && !selected.some(value => constraintKey(value) === constraintKey(displayName))) {
            selected.push(displayName);
        }
        renderTripConstraints();
        renderCustomConstraintCheckboxes(selected);
        renderProfileConstraintCheckboxes(state.profile?.customConstraints || []);
        renderExpenseCustomAmountRows();
        showMessage("Contrainte ajoutée au voyage.");
        return displayName;
    } catch (error) {
        showMessage(`Contrainte refusée : ${error.message}`, "error");
        return null;
    }
}

function clearSessionState() {
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
}

async function afterAuthentication(message) {
    writeJson("achabitation.user", state.user);
    hydrateUserUi();
    await loadProfile();
    await loadTrips();
    const selected = state.selectedTrip?.id ? state.trips.find(t => t.id === state.selectedTrip.id) : null;
    if (selected) {
        state.selectedTrip = selected;
        writeJson("achabitation.selectedTrip", selected);
        await loadTripData();
    } else if (state.trips.length > 0) {
        await selectTrip(state.trips[0].id);
    } else {
        state.selectedTrip = null;
        writeJson("achabitation.selectedTrip", null);
        renderAll();
    }
    showMessage(message);
}

async function loginUser() {
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
        await afterAuthentication("Compte connecté.");
    } catch (error) {
        showMessage(`Connexion refusée : ${error.message}`, "error");
    }
}

async function createUser() {
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
        await afterAuthentication("Compte créé et connecté.");
    } catch (error) {
        showMessage(`Création du compte refusée : ${error.message}`, "error");
    }
}

async function updateAccount(event) {
    event.preventDefault();
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant de modifier ton compte.", "error");
        return;
    }
    const payload = {
        email: $("accountEmail").value.trim(),
        displayName: $("accountDisplayName").value.trim()
    };
    try {
        state.user = await api("/auth/account", { method: "PUT", body: JSON.stringify(payload) });
        writeJson("achabitation.user", state.user);
        hydrateUserUi();
        hidePanel("accountEditForm");
        if (state.profile) {
            state.profile.email = state.user.email;
            state.profile.displayName = state.user.displayName;
            writeJson("achabitation.profile", state.profile);
        }
        showMessage("Compte mis à jour.");
    } catch (error) {
        showMessage(`Modification du compte refusée : ${error.message}`, "error");
    }
}

function logoutUser() {
    clearSessionState();
    hydrateUserUi();
    renderAll();
    activateTab("dashboard");
    openProfilePanel();
    showMessage("Compte déconnecté localement.");
}


async function loadTrips() {
    if (!state.user?.devToken) {
        state.trips = [];
        state.selectedTrip = null;
        renderTrips();
        return;
    }
    state.trips = asArray(await api("/trips"));
    renderTrips();
}

async function createTrip(event) {
    event.preventDefault();
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant de créer un voyage.", "error");
        return;
    }
    const payload = {
        name: $("tripName").value.trim(),
        startDate: $("tripStartDate").value || null,
        endDate: $("tripEndDate").value || null,
        referenceCurrency: $("tripCurrency").value.trim().toUpperCase() || "EUR",
        ownerUserId: state.user.userId,
        customConstraints: []
    };

    try {
        validateTripPayload(payload);
        const trip = await api("/trips", { method: "POST", body: JSON.stringify(payload) });
        $("tripForm").reset();
        $("tripCurrency").value = payload.referenceCurrency;
        hidePanel("tripFormPanel");
        await loadTrips();
        await selectTrip(trip.id);
        showMessage("Voyage créé.");
    } catch (error) {
        showMessage(`Création du voyage refusée : ${error.message}`, "error");
    }
}

async function joinTripByInvitationCode() {
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant de rejoindre un voyage.", "error");
        openProfilePanel();
        return;
    }
    const code = ($("joinInvitationCode")?.value || "").trim();
    if (!code) {
        showMessage("Entre un code d’invitation.", "error");
        return;
    }

    try {
        const trip = await api("/trips/join-by-code", {
            method: "POST",
            body: JSON.stringify({ invitationCode: code, applyProfileToGuest: false })
        });
        if ($("joinInvitationCode")) $("joinInvitationCode").value = "";
        await loadTrips();
        await selectTrip(trip.id);
        showMessage(`Voyage rejoint : ${trip.name}.`);
    } catch (error) {
        showMessage(`Impossible de rejoindre le voyage : ${error.message}`, "error");
    }
}

async function selectTrip(tripId) {
    state.selectedTrip = state.trips.find(t => t.id === tripId) || null;
    writeJson("achabitation.selectedTrip", state.selectedTrip);
    resetPersonForm();
    resetExpenseForm();
    await loadTripData();
}

async function loadTripData() {
    if (!state.selectedTrip?.id) {
        renderAll();
        return;
    }
    await Promise.all([loadPersons(false), loadExpenses(false), loadSummary(false), loadAuditLogs(false), loadInvitations(false)]);
    renderAll();
}

async function refreshCurrent() {
    await loadTrips();
    if (state.selectedTrip?.id) {
        const refreshed = state.trips.find(t => t.id === state.selectedTrip.id);
        if (refreshed) state.selectedTrip = refreshed;
        await loadTripData();
    } else {
        renderAll();
    }
    showMessage("Données rafraîchies.");
}

async function loadInvitations(render = true) {
    if (!state.selectedTrip?.id || !state.user?.devToken) {
        state.invitations = [];
        if (render) renderInvitations();
        return;
    }
    try {
        state.invitations = await api(`/trips/${state.selectedTrip.id}/invitations`);
    } catch (error) {
        state.invitations = [];
        // Les participant·es non admin n'ont pas accès aux invitations. Ce n'est pas une erreur bloquante d'affichage.
    }
    if (render) renderInvitations();
}

async function createInvitation() {
    if (!ensureTripSelected()) return;
    try {
        const roleToGrant = $("inviteRole")?.value || "PARTICIPANT";
        await api(`/trips/${state.selectedTrip.id}/invitations`, {
            method: "POST",
            body: JSON.stringify({ roleToGrant, expiresInDays: 7 })
        });
        await loadInvitations();
        showMessage("Invitation créée.");
    } catch (error) {
        showMessage(`Invitation refusée : ${error.message}`, "error");
    }
}

async function revokeInvitation(invitationId) {
    if (!ensureTripSelected()) return;
    if (!confirm("Révoquer cette invitation ?")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/invitations/${invitationId}`, { method: "DELETE" });
        await loadInvitations();
        showMessage("Invitation révoquée.");
    } catch (error) {
        showMessage(`Révocation refusée : ${error.message}`, "error");
    }
}

function renderInvitations() {
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
            <strong>${escapeHtml(invitation.code)}</strong>
            <span class="small">Rôle : ${escapeHtml(invitation.roleToGrant)} · Expire : ${dateFr((invitation.expiresAt || "").slice(0,10))} · ${invitation.revoked ? "Révoquée" : invitation.usable ? "Active" : "Expirée"}</span>
            <div class="row-actions"><button type="button" class="danger small-button" data-revoke-invite="${escapeHtml(invitation.id)}">Révoquer</button></div>
        </div>
    `).join("");
    container.querySelectorAll("[data-revoke-invite]").forEach(button => {
        button.addEventListener("click", () => revokeInvitation(button.dataset.revokeInvite));
    });
}

async function downloadExport(fileName) {
    if (!ensureTripSelected()) return;
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant d’exporter les données.", "error");
        return;
    }
    try {
        const response = await fetch(`${API}/trips/${state.selectedTrip.id}/exports/${fileName}`, {
            headers: { "Authorization": `Bearer ${state.user.devToken}` }
        });
        if (!response.ok) {
            const body = await response.json().catch(() => null);
            throw new Error(body?.details?.join("\n") || `${response.status} ${response.statusText}`);
        }
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        showMessage("Export généré.");
    } catch (error) {
        showMessage(`Export refusé : ${error.message}`, "error");
    }
}

function renderAll() {
    renderTrips();
    renderSelectedTrip();
    renderDashboard();
    renderSettings();
    renderPersons();
    renderTripConstraints();
    renderCustomConstraintCheckboxes();
    renderProfile();
    renderExpensePersonOptions();
    renderExpenseCustomAmountRows();
    renderExpenses();
    renderSummary();
    renderAuditLogs();
    renderInvitations();
    if (!document.querySelector(".presence-period-row")) renderPresencePeriodRows();
    updatePresencePeriodDateBounds();
    updatePersonFieldState();
    updateExpenseFieldState();
}

function renderTrips() {
    const list = $("tripList");
    if (!state.trips.length) {
        list.innerHTML = `<div class="list-item"><strong>Aucun voyage</strong><span class="small">Crée un premier voyage.</span></div>`;
        return;
    }
    list.innerHTML = state.trips.map(trip => `
        <div class="list-item ${state.selectedTrip?.id === trip.id ? "active" : ""}" data-trip-id="${trip.id}">
            <strong>${escapeHtml(trip.name)}</strong>
            <span class="small">${dateFr(trip.startDate)} → ${dateFr(trip.endDate)} · ${trip.referenceCurrency}</span>
        </div>
    `).join("");
    list.querySelectorAll("[data-trip-id]").forEach(item => {
        item.addEventListener("click", () => selectTrip(item.dataset.tripId));
    });
}

function renderSelectedTrip() {
    if (!state.selectedTrip) {
        $("selectedTripTitle").textContent = "Aucun voyage sélectionné";
        $("selectedTripMeta").textContent = "Crée ou sélectionne un voyage pour commencer.";
        if ($("topSelectedTripName")) $("topSelectedTripName").textContent = "Aucun voyage";
        $("expenseDate").min = "";
        $("expenseDate").max = "";
        return;
    }
    $("selectedTripTitle").textContent = state.selectedTrip.name;
    $("selectedTripMeta").textContent = `${dateFr(state.selectedTrip.startDate)} → ${dateFr(state.selectedTrip.endDate)} · Devise ${state.selectedTrip.referenceCurrency}`;
    if ($("topSelectedTripName")) $("topSelectedTripName").textContent = state.selectedTrip.name;
    $("expenseCurrency").value = state.selectedTrip.referenceCurrency || "EUR";
    $("expenseDate").min = state.selectedTrip.startDate || "";
    $("expenseDate").max = state.selectedTrip.endDate || "";
    updatePresencePeriodDateBounds();
}

function renderDashboard() {
    const activePersons = state.persons.filter(person => person.active).length;
    const totalExpenses = state.expenses.reduce((sum, expense) => sum + Number(expense.totalAmount || 0) * Number(expense.exchangeRateToTripCurrency || 1), 0);
    if ($("metricPersons")) $("metricPersons").textContent = String(activePersons);
    if ($("metricExpenses")) $("metricExpenses").textContent = String(state.expenses.length);
    if ($("metricTotalExpenses")) $("metricTotalExpenses").textContent = money(totalExpenses, selectedCurrency());
    if ($("metricCurrency")) $("metricCurrency").textContent = selectedCurrency();
}

function renderSettings() {
    if (!state.selectedTrip) {
        if ($("settingsTripName")) $("settingsTripName").textContent = "—";
        if ($("settingsTripDates")) $("settingsTripDates").textContent = "—";
        if ($("settingsTripCurrency")) $("settingsTripCurrency").textContent = "—";
        return;
    }
    if ($("settingsTripName")) $("settingsTripName").textContent = state.selectedTrip.name;
    if ($("settingsTripDates")) $("settingsTripDates").textContent = `${dateFr(state.selectedTrip.startDate)} → ${dateFr(state.selectedTrip.endDate)}`;
    if ($("settingsTripCurrency")) $("settingsTripCurrency").textContent = state.selectedTrip.referenceCurrency || "EUR";
}

async function loadPersons(render = true) {
    if (!state.selectedTrip?.id) return;
    state.persons = await api(`/trips/${state.selectedTrip.id}/persons`);
    if (render) {
        renderPersons();
        renderExpensePersonOptions();
    }
}

function resetPersonForm() {
    $("personForm").reset();
    $("personId").value = "";
    $("personFormTitle").textContent = "Ajouter une personne";
    $("personWeightMode").value = "LIVING_REST";
    $("personLivingRest").value = "1000";
    $("personActive").checked = true;
    renderPresencePeriodRows(defaultPresencePeriods());
    renderCustomConstraintCheckboxes([]);
    updatePersonFieldState();
}

function updatePersonFieldState() {
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
}

function calculateAdvancedRav() {
    const income = asNumber("netIncomeAfterTax") || 0;
    const charges = ["rent", "credits", "fixedCharges", "transport", "insurance", "otherMandatoryExpenses", "menstrualProtection"]
        .map(id => asNumber(id) || 0)
        .reduce((a, b) => a + b, 0);
    const rav = Math.max(0, income - charges);
    setNumber("personLivingRest", rav.toFixed(2));
    showMessage(`Reste à vivre calculé : ${rav.toFixed(2)} €`);
}

function selectedCustomConstraintsFromForm() {
    return Array.from(document.querySelectorAll("#customConstraintCheckboxes input:checked"))
        .map(input => canonicalConstraintName(input.value))
        .filter(Boolean);
}

function renderCustomConstraintCheckboxes(selected = null) {
    const container = $("customConstraintCheckboxes");
    if (!container) return;
    const selectedSet = new Set((selected ?? selectedCustomConstraintsFromForm()).map(constraintKey));
    const names = selectedTripConstraintNames();
    if (!names.length) {
        container.innerHTML = `<p class="small">Aucune contrainte personnalisée pour ce voyage.</p>`;
        return;
    }
    container.innerHTML = names.map(name => `
        <label class="checkbox"><input type="checkbox" value="${escapeHtml(name)}" ${selectedSet.has(constraintKey(name)) ? "checked" : ""}> ${escapeHtml(name)}</label>
    `).join("");
}

async function addCustomConstraintFromForm() {
    await addCustomConstraintFromInput($("newCustomConstraintName"), true);
}

function parsePresencePeriods() {
    const periods = collectPresencePeriodInputs()
        .map((period, index) => ({ ...period, originalIndex: index + 1 }))
        .filter(period => period.startDate || period.endDate);

    if (!periods.length) throw new Error("Au moins une période de présence est obligatoire.");

    periods.forEach(period => {
        if (!period.startDate || !period.endDate) {
            throw new Error(`Période ${period.originalIndex} invalide : début et fin sont obligatoires.`);
        }
        if (isAfterIso(period.startDate, period.endDate)) {
            throw new Error(`Période ${period.originalIndex} invalide : le début est après la fin.`);
        }
        validateDateInsideSelectedTrip(period.startDate, `La période de présence ${period.originalIndex}`);
        validateDateInsideSelectedTrip(period.endDate, `La période de présence ${period.originalIndex}`);
    });

    const sorted = periods
        .map(period => ({ startDate: period.startDate, endDate: period.endDate }))
        .sort((a, b) => a.startDate.localeCompare(b.startDate));

    let previous = null;
    sorted.forEach(period => {
        if (previous && !isAfterIso(period.startDate, previous.endDate)) {
            throw new Error("Les périodes de présence d'une même personne ne doivent pas se chevaucher.");
        }
        previous = period;
    });

    return sorted;
}

function personPayload(includeActive) {
    const averageMode = $("personWeightMode").value === "AVERAGE";
    const advanced = $("personAdvancedRav").checked && !averageMode;
    const payload = {
        name: $("personName").value.trim(),
        livingRest: averageMode ? 0 : (advanced ? null : asNumber("personLivingRest")),
        weightMode: $("personWeightMode").value,
        advancedLivingRest: advanced,
        netIncomeAfterTax: advanced ? asNumber("netIncomeAfterTax") : null,
        rent: advanced ? asNumber("rent") : null,
        credits: advanced ? asNumber("credits") : null,
        fixedCharges: advanced ? asNumber("fixedCharges") : null,
        transport: advanced ? asNumber("transport") : null,
        insurance: advanced ? asNumber("insurance") : null,
        otherMandatoryExpenses: advanced ? asNumber("otherMandatoryExpenses") : null,
        menstrualProtection: advanced ? asNumber("menstrualProtection") : null,
        vegetarian: $("personVegetarian").checked,
        noAlcohol: $("personNoAlcohol").checked,
        livingRestPublic: state.profile?.livingRestPublic || false,
        customConstraints: selectedCustomConstraintsFromForm(),
        presencePeriods: parsePresencePeriods()
    };
    if (includeActive) payload.active = $("personActive").checked;
    return payload;
}

async function savePerson(event) {
    event.preventDefault();
    if (!ensureTripSelected()) return;
    try {
        const id = $("personId").value;
        if (id) {
            await api(`/trips/${state.selectedTrip.id}/persons/${id}`, {
                method: "PUT",
                body: JSON.stringify(personPayload(true))
            });
            showMessage("Personne modifiée.");
        } else {
            await api(`/trips/${state.selectedTrip.id}/persons`, {
                method: "POST",
                body: JSON.stringify(personPayload(false))
            });
            showMessage("Personne ajoutée.");
        }
        resetPersonForm();
        hidePanel("personFormPanel");
        await loadTripData();
    } catch (error) {
        showMessage(`Enregistrement refusé : ${error.message}`, "error");
    }
}

function editPerson(id) {
    const person = state.persons.find(p => p.id === id);
    if (!person) return;
    $("personId").value = person.id;
    $("personFormTitle").textContent = `Modifier ${person.name}`;
    $("personName").value = person.name;
    $("personWeightMode").value = person.weightMode || "LIVING_REST";
    setNumber("personLivingRest", person.livingRestHidden ? "" : (person.livingRest ?? 0));
    $("personAdvancedRav").checked = !!person.advancedLivingRest;
    setNumber("netIncomeAfterTax", person.livingRestHidden ? "" : (person.netIncomeAfterTax ?? 0));
    setNumber("rent", person.livingRestHidden ? "" : (person.rent ?? 0));
    setNumber("credits", person.livingRestHidden ? "" : (person.credits ?? 0));
    setNumber("fixedCharges", person.livingRestHidden ? "" : (person.fixedCharges ?? 0));
    setNumber("transport", person.livingRestHidden ? "" : (person.transport ?? 0));
    setNumber("insurance", person.livingRestHidden ? "" : (person.insurance ?? 0));
    setNumber("otherMandatoryExpenses", person.livingRestHidden ? "" : (person.otherMandatoryExpenses ?? 0));
    setNumber("menstrualProtection", person.livingRestHidden ? "" : (person.menstrualProtection ?? 0));
    renderCustomConstraintCheckboxes(person.customConstraints || []);
    $("personVegetarian").checked = !!person.vegetarian;
    $("personNoAlcohol").checked = !!person.noAlcohol;
    $("personActive").checked = !!person.active;
    renderPresencePeriodRows(person.presencePeriods || defaultPresencePeriods());
    updatePersonFieldState();
    activateTab("persons");
    showPanel("personFormPanel");
    $("personFormPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function disablePerson(id) {
    if (!confirm("Désactiver cette personne ? Elle restera conservée pour l’historique.")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/persons/${id}`, { method: "DELETE" });
        await loadTripData();
        showMessage("Personne désactivée.");
    } catch (error) {
        showMessage(`Désactivation refusée : ${error.message}`, "error");
    }
}

async function linkPersonToCurrentUser(id) {
    if (!state.user?.devToken) {
        showMessage("Connecte-toi avant de lier un guest à ton compte.", "error");
        return;
    }
    const person = state.persons.find(p => p.id === id);
    if (!person) return;
    if (!state.profile) await loadProfile(false);

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
        await loadProfile(false);
        await loadTripData();
        showMessage(applyProfileToGuest ? "Guest lié à ton compte et profil appliqué." : "Guest lié à ton compte sans modifier ses données.");
    } catch (error) {
        showMessage(`Liaison refusée : ${error.message}`, "error");
    }
}

function renderPersons() {
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
            .filter(Boolean).map(o => `<span class="badge">${escapeHtml(o)}</span>`).join("") || "—";
        const periods = (person.presencePeriods || []).map(p => `${dateFr(p.startDate)} → ${dateFr(p.endDate)}`).join("<br>");
        const linkButton = state.user?.devToken && person.guest
            ? `<button class="secondary small-button" type="button" data-link-person="${person.id}">Lier à mon compte</button>`
            : "";
        return `
            <tr>
                <td><strong>${escapeHtml(person.name)}</strong></td>
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
    tbody.querySelectorAll("[data-edit-person]").forEach(b => b.addEventListener("click", () => editPerson(b.dataset.editPerson)));
    tbody.querySelectorAll("[data-link-person]").forEach(b => b.addEventListener("click", () => linkPersonToCurrentUser(b.dataset.linkPerson)));
    tbody.querySelectorAll("[data-disable-person]").forEach(b => b.addEventListener("click", () => disablePerson(b.dataset.disablePerson)));
}

async function loadExpenses(render = true) {
    if (!state.selectedTrip?.id) return;
    state.expenses = await api(`/trips/${state.selectedTrip.id}/expenses`);
    if (render) renderExpenses();
}

function resetExpenseForm() {
    $("expenseForm").reset();
    $("expenseId").value = "";
    $("expenseFormTitle").textContent = "Ajouter une dépense";
    const defaultDate = state.selectedTrip?.startDate && isBeforeIso(todayIso(), state.selectedTrip.startDate)
        ? state.selectedTrip.startDate
        : state.selectedTrip?.endDate && isAfterIso(todayIso(), state.selectedTrip.endDate)
            ? state.selectedTrip.endDate
            : todayIso();
    $("expenseDate").value = defaultDate;
    $("expenseType").value = "NORMAL";
    $("expenseCurrency").value = selectedCurrency();
    $("expenseExchangeRate").value = "1";
    renderExpensePersonOptions();
    renderExpenseCustomAmountRows({});
    updateExpenseFieldState();
}

function renderExpensePersonOptions() {
    const payer = $("expensePayer");
    const activePersons = state.persons.filter(p => p.active);
    payer.innerHTML = activePersons.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join("");
    renderManualParticipants();
    renderExpenseCustomAmountRows();
}

function collectExpenseCustomAmounts() {
    const result = {};
    document.querySelectorAll(".expense-custom-amount").forEach(input => {
        const name = input.dataset.constraintName;
        const amount = Number(input.value || 0);
        if (name && amount > 0) {
            result[name] = amount;
        }
    });
    return result;
}

function renderExpenseCustomAmountRows(values = null) {
    const container = $("expenseCustomAmountRows");
    if (!container) return;
    const currentValues = values ?? collectExpenseCustomAmounts();
    const names = selectedTripConstraintNames();
    if (!names.length) {
        container.innerHTML = `<p class="small">Aucune contrainte personnalisée disponible. Ajoute-la dans les contraintes du voyage.</p>`;
        return;
    }
    container.innerHTML = names.map(name => {
        const matchingKey = Object.keys(currentValues || {}).find(key => constraintKey(key) === constraintKey(name));
        const value = matchingKey ? currentValues[matchingKey] : 0;
        return `
            <label>${escapeHtml(name)}
                <input class="expense-custom-amount" data-constraint-name="${escapeHtml(name)}" type="number" min="0" step="0.01" value="${value || 0}">
            </label>
        `;
    }).join("");
}

function renderManualParticipants() {
    const block = $("manualParticipantsBlock");
    const checked = $("expenseAdvancedMode").checked;
    block.classList.toggle("hidden", !checked);
    const selectedIds = new Set(Array.from(document.querySelectorAll("#manualParticipants input:checked")).map(i => i.value));
    const activePersons = state.persons.filter(p => p.active);
    $("manualParticipants").innerHTML = activePersons.map(p => `
        <label class="checkbox"><input type="checkbox" value="${p.id}" ${selectedIds.has(p.id) ? "checked" : ""}> ${escapeHtml(p.name)}</label>
    `).join("") || `<p class="small">Ajoute d’abord des personnes actives.</p>`;
}

function updateExpenseFieldState() {
    const global = $("expenseType").value === "GLOBAL";
    $("expenseMeat").disabled = global;
    $("expenseAlcohol").disabled = global;
    document.querySelectorAll(".expense-custom-amount").forEach(input => input.disabled = global);
    $("expenseCustomAmountsBlock").classList.toggle("muted-block", global);
    if (global) {
        $("expenseMeat").value = 0;
        $("expenseAlcohol").value = 0;
        document.querySelectorAll(".expense-custom-amount").forEach(input => input.value = 0);
    }
    renderManualParticipants();
}

function expensePayload() {
    const manualParticipantIds = Array.from(document.querySelectorAll("#manualParticipants input:checked")).map(input => input.value);
    const global = $("expenseType").value === "GLOBAL";
    return {
        title: $("expenseTitle").value.trim(),
        date: $("expenseDate").value,
        payerPersonId: $("expensePayer").value,
        totalAmount: asNumber("expenseTotal"),
        meatAmount: global ? 0 : (asNumber("expenseMeat") || 0),
        alcoholAmount: global ? 0 : (asNumber("expenseAlcohol") || 0),
        customConstraintAmounts: global ? {} : collectExpenseCustomAmounts(),
        type: $("expenseType").value,
        advancedMode: $("expenseAdvancedMode").checked,
        manualParticipantIds,
        currency: $("expenseCurrency").value.trim().toUpperCase() || selectedCurrency(),
        exchangeRateToTripCurrency: asNumber("expenseExchangeRate") || 1
    };
}

async function saveExpense(event) {
    event.preventDefault();
    if (!ensureTripSelected()) return;
    if (!state.persons.filter(p => p.active).length) {
        showMessage("Ajoute au moins une personne active avant une dépense.", "error");
        return;
    }
    try {
        const payload = expensePayload();
        validateDateInsideSelectedTrip(payload.date, "La date de dépense");
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
        resetExpenseForm();
        hidePanel("expenseFormPanel");
        await loadTripData();
    } catch (error) {
        showMessage(`Enregistrement refusé : ${error.message}`, "error");
    }
}

function editExpense(id) {
    const expense = state.expenses.find(e => e.id === id);
    if (!expense) return;
    $("expenseId").value = expense.id;
    $("expenseFormTitle").textContent = `Modifier ${expense.title}`;
    $("expenseTitle").value = expense.title;
    $("expenseDate").value = expense.date;
    $("expensePayer").value = expense.payerPersonId;
    setNumber("expenseTotal", expense.totalAmount);
    setNumber("expenseMeat", expense.meatAmount || 0);
    setNumber("expenseAlcohol", expense.alcoholAmount || 0);
    renderExpenseCustomAmountRows(expense.customConstraintAmounts || {});
    $("expenseType").value = expense.type || "NORMAL";
    $("expenseAdvancedMode").checked = !!expense.advancedMode;
    $("expenseCurrency").value = expense.currency || selectedCurrency();
    setNumber("expenseExchangeRate", expense.exchangeRateToTripCurrency || 1);
    renderManualParticipants();
    const manualIds = new Set(expense.manualParticipantIds || []);
    document.querySelectorAll("#manualParticipants input").forEach(input => input.checked = manualIds.has(input.value));
    updateExpenseFieldState();
    activateTab("expenses");
    showPanel("expenseFormPanel");
    $("expenseFormPanel")?.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function deleteExpense(id) {
    if (!confirm("Supprimer cette dépense ?")) return;
    try {
        await api(`/trips/${state.selectedTrip.id}/expenses/${id}`, { method: "DELETE" });
        await loadTripData();
        showMessage("Dépense supprimée.");
    } catch (error) {
        showMessage(`Suppression refusée : ${error.message}`, "error");
    }
}

function canParticipateInAllocation(person) {
    if (!person?.active) return false;
    if (person.livingRestHidden) return true;
    if (person.weightMode === "AVERAGE") return true;
    return Number(person.livingRest ?? 0) > 0;
}

function isPersonPresentOn(person, date) {
    if (!person || !date) return false;
    return (person.presencePeriods || []).some(period =>
        period.startDate && period.endDate
        && !isAfterIso(period.startDate, date)
        && !isBeforeIso(period.endDate, date)
    );
}

function uniquePersons(persons) {
    const seen = new Set();
    return persons.filter(person => {
        if (!person?.id || seen.has(person.id)) return false;
        seen.add(person.id);
        return true;
    });
}

function concernedPersonsForExpense(expense) {
    const eligible = state.persons.filter(canParticipateInAllocation);

    if (expense.advancedMode) {
        const selectedIds = new Set(expense.manualParticipantIds || []);
        return eligible.filter(person => selectedIds.has(person.id));
    }

    if (expense.type === "GLOBAL") {
        return eligible;
    }

    const present = eligible.filter(person => isPersonPresentOn(person, expense.date));
    const meat = Number(expense.meatAmount || 0);
    const alcohol = Number(expense.alcoholAmount || 0);
    const total = Number(expense.totalAmount || 0);
    const customAmounts = expense.customConstraintAmounts || {};
    const customTotal = Object.values(customAmounts).map(Number).reduce((a, b) => a + (b || 0), 0);
    const general = Math.max(0, total - meat - alcohol - customTotal);

    const concerned = [];
    if (general > 0) concerned.push(...present);
    if (meat > 0) concerned.push(...present.filter(person => !person.vegetarian));
    if (alcohol > 0) concerned.push(...present.filter(person => !person.noAlcohol));
    Object.entries(customAmounts).forEach(([constraintName, amount]) => {
        if (Number(amount || 0) > 0) {
            concerned.push(...present.filter(person => !personHasCustomConstraint(person, constraintName)));
        }
    });

    return uniquePersons(concerned);
}

function renderConcernedPersons(expense) {
    const persons = concernedPersonsForExpense(expense);
    if (!persons.length) {
        return `<span class="badge warning">Aucune</span><br><span class="small">Dépense invalide ou données incomplètes</span>`;
    }

    const label = expense.advancedMode
        ? "Mode avancé"
        : expense.type === "GLOBAL"
            ? "Dépense globale"
            : "Selon date / végé / alcool / contraintes";

    const names = persons.map(person => person.name).join(", ");
    const chips = persons.map(person => `<span class="person-chip">${escapeHtml(person.name)}</span>`).join("");
    return `<div class="participant-list" title="${escapeHtml(names)}">${chips}</div><span class="small">${persons.length} personne${persons.length > 1 ? "s" : ""} · ${label}</span>`;
}

function renderExpenseDetails(expense) {
    const details = [
        `Viande ${money(expense.meatAmount || 0, expense.currency)}`,
        `Alcool ${money(expense.alcoholAmount || 0, expense.currency)}`,
        ...Object.entries(expense.customConstraintAmounts || {})
            .filter(([, amount]) => Number(amount || 0) > 0)
            .map(([name, amount]) => `${escapeHtml(name)} ${money(amount, expense.currency)}`)
    ];
    return details.join(" · ");
}

function renderExpenses() {
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
            <td><strong>${escapeHtml(expense.title)}</strong><br><span class="small">${renderExpenseDetails(expense)}</span></td>
            <td>${dateFr(expense.date)}</td>
            <td>${escapeHtml(expense.payerName || "—")}</td>
            <td>${money(expense.totalAmount, expense.currency)}<br><span class="small">Taux ${expense.exchangeRateToTripCurrency || 1}</span></td>
            <td><span class="badge">${expense.type}</span>${expense.advancedMode ? `<span class="badge">Avancé</span>` : ""}</td>
            <td>${renderConcernedPersons(expense)}</td>
            <td><div class="row-actions">
                <button class="secondary small-button" type="button" data-edit-expense="${expense.id}">Modifier</button>
                <button class="danger small-button" type="button" data-delete-expense="${expense.id}">Supprimer</button>
            </div></td>
        </tr>
    `).join("");
    tbody.querySelectorAll("[data-edit-expense]").forEach(b => b.addEventListener("click", () => editExpense(b.dataset.editExpense)));
    tbody.querySelectorAll("[data-delete-expense]").forEach(b => b.addEventListener("click", () => deleteExpense(b.dataset.deleteExpense)));
}

async function loadSummary(render = true) {
    if (!state.selectedTrip?.id) return;
    try {
        state.summary = await api(`/trips/${state.selectedTrip.id}/summary`);
        if (render) renderSummary();
    } catch (error) {
        showMessage(`Résumé indisponible : ${error.message}`, "error");
    }
}

function renderSummary() {
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
                <td>${escapeHtml(balance.personName)}</td>
                <td>${money(balance.totalPaid)}</td>
                <td>${money(balance.totalOwed)}</td>
                <td class="${cls}">${money(balance.balance)}</td>
            </tr>`;
        }).join("");
    }
    $("settlementsList").innerHTML = settlements.length
        ? settlements.map(s => `<div class="list-item"><strong>${escapeHtml(s.fromPersonName)} rembourse ${money(s.amount)} à ${escapeHtml(s.toPersonName)}</strong></div>`).join("")
        : `<div class="list-item"><strong>Aucun remboursement nécessaire</strong><span class="small">Les soldes sont déjà équilibrés ou aucune dépense n’a été saisie.</span></div>`;
}

async function loadAuditLogs(render = true) {
    if (!state.selectedTrip?.id) return;
    try {
        state.auditLogs = await api(`/trips/${state.selectedTrip.id}/audit-logs`);
        if (render) renderAuditLogs();
    renderInvitations();
    } catch (error) {
        showMessage(`Historique indisponible : ${error.message}`, "error");
    }
}

function renderAuditLogs() {
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
            <strong>${escapeHtml(log.action || "ACTION")}</strong>
            <p class="small">${new Date(log.createdAt).toLocaleString("fr-FR")} · ${escapeHtml(log.entityType || "")}</p>
            <p>${escapeHtml(log.description || "")}</p>
        </div>
    `).join("");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

window.addEventListener("DOMContentLoaded", () => {
    init().catch(error => showMessage(error.message, "error"));
});
