// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.loadTrips = async function () {
    if (!state.user?.accessToken) {
        state.trips = [];
        state.selectedTrip = null;
        ctx.renderTrips();
        return;
    }
    state.trips = asArray(await api("/trips"));
    ctx.renderTrips();
};

ctx.createTrip = async function (event) {
    event.preventDefault();
    if (!state.user?.accessToken) {
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
        ctx.validateTripPayload(payload);
        const trip = await api("/trips", { method: "POST", body: JSON.stringify(payload) });
        $("tripForm").reset();
        $("tripCurrency").value = payload.referenceCurrency;
        ctx.hidePanel("tripFormPanel");
        await ctx.loadTrips();
        await ctx.selectTrip(trip.id);
        showMessage("Voyage créé.");
    } catch (error) {
        showMessage(`Création du voyage refusée : ${error.message}`, "error");
    }
};

ctx.joinTripByInvitationCode = async function () {
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant de rejoindre un voyage.", "error");
        ctx.openProfilePanel();
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
        await ctx.loadTrips();
        await ctx.selectTrip(trip.id);
        state.pendingClaimTripId = trip.id;
        writeJson("achabitation.pendingClaimTripId", state.pendingClaimTripId);
        ctx.renderPostJoinClaimPanel();
        showMessage(`Voyage rejoint : ${trip.name}. Choisis maintenant à quelle personne te rattacher.`);
    } catch (error) {
        showMessage(`Impossible de rejoindre le voyage : ${error.message}`, "error");
    }
};

ctx.hasUsableProfileForPersonCreation = function () {
    if (!state.profile) return false;
    if (state.profile.weightMode === "AVERAGE") return true;
    return Number(state.profile.livingRest ?? 0) > 0;
};

ctx.currentUserAlreadyLinkedInSelectedTrip = function () {
    if (!state.user?.userId) return false;
    return state.persons.some(person => person.linkedUserId === state.user.userId && person.active !== false);
};

ctx.renderPostJoinClaimPanel = function () {
    const panel = $("postJoinClaimPanel");
    const container = $("postJoinGuestOptions");
    if (!panel || !container) return;
    if (!state.user?.accessToken || !state.selectedTrip?.id || state.pendingClaimTripId !== state.selectedTrip.id || ctx.currentUserAlreadyLinkedInSelectedTrip()) {
        panel.classList.add("hidden");
        return;
    }

    const guests = state.persons.filter(person => person.active !== false && person.guest);
    if (!guests.length) {
        container.innerHTML = `<div class="list-item"><strong>Aucun guest disponible</strong><span class="small">Crée une nouvelle personne directement liée à ton compte.</span></div>`;
    } else {
        container.innerHTML = guests.map(person => `
            <div class="list-item claim-guest-item">
                <div>
                    <strong>${ctx.escapeHtml(person.name)}</strong>
                    <span class="small">${(person.presencePeriods || []).map(p => `${dateFr(p.startDate)} → ${dateFr(p.endDate)}`).join(" · ") || "Présence non renseignée"}</span>
                </div>
                <button class="secondary small-button" type="button" data-post-join-link="${person.id}">C’est moi</button>
            </div>
        `).join("");
        container.querySelectorAll("[data-post-join-link]").forEach(button => {
            button.addEventListener("click", () => ctx.linkPersonToCurrentUser(button.dataset.postJoinLink));
        });
    }
    panel.classList.remove("hidden");
};

ctx.createCurrentUserPerson = async function () {
    if (!ctx.ensureTripSelected()) return;
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant de t’ajouter au voyage.", "error");
        ctx.openProfilePanel();
        return;
    }
    if (ctx.currentUserAlreadyLinkedInSelectedTrip()) {
        showMessage("Ton compte est déjà lié à une personne dans ce voyage.", "error");
        return;
    }
    if (!state.profile) await ctx.loadProfile(false);

    const profileUsable = ctx.hasUsableProfileForPersonCreation();
    let applyProfileToPerson = false;
    if (profileUsable) {
        applyProfileToPerson = confirm(
            "Créer une personne à ton nom dans ce voyage ?\n\n" +
            "OK : créer avec les données de ton profil actuel.\n" +
            "Annuler : créer en mode moyenne, sans appliquer ton RAV. Tu pourras modifier ensuite."
        );
    } else {
        const proceed = confirm(
            "Ton profil RAV est nul ou incomplet. La personne sera créée en mode moyenne, sans appliquer ton RAV. Continuer ?"
        );
        if (!proceed) return;
    }

    try {
        await api(`/trips/${state.selectedTrip.id}/persons/current-user`, {
            method: "POST",
            body: JSON.stringify({
                name: state.user.displayName || state.profile?.displayName || "Moi",
                applyProfileToPerson,
                presencePeriods: ctx.defaultPresencePeriods()
            })
        });
        state.pendingClaimTripId = null;
        writeJson("achabitation.pendingClaimTripId", null);
        ctx.hidePanel("postJoinClaimPanel");
        await ctx.loadProfile(false);
        await ctx.loadTripData();
        showMessage(applyProfileToPerson ? "Tu as été ajouté·e au voyage avec ton profil." : "Tu as été ajouté·e au voyage en mode moyenne.");
    } catch (error) {
        showMessage(`Ajout refusé : ${error.message}`, "error");
    }
};

ctx.selectTrip = async function (tripId) {
    state.selectedTrip = state.trips.find(t => t.id === tripId) || null;
    writeJson("achabitation.selectedTrip", state.selectedTrip);
    ctx.resetPersonForm();
    ctx.resetExpenseForm();
    await ctx.loadTripData();
};

ctx.loadTripData = async function () {
    if (!state.selectedTrip?.id) {
        ctx.renderAll();
        return;
    }
    await Promise.all([ctx.loadPersons(false), ctx.loadExpenses(false), ctx.loadSummary(false), ctx.loadAuditLogs(false), ctx.loadInvitations(false)]);
    ctx.renderAll();
};

ctx.refreshCurrent = async function () {
    await ctx.loadTrips();
    if (state.selectedTrip?.id) {
        const refreshed = state.trips.find(t => t.id === state.selectedTrip.id);
        if (refreshed) state.selectedTrip = refreshed;
        await ctx.loadTripData();
    } else {
        ctx.renderAll();
    }
    showMessage("Données rafraîchies.");
};

ctx.downloadExport = async function (fileName) {
    if (!ctx.ensureTripSelected()) return;
    if (!state.user?.accessToken) {
        showMessage("Connecte-toi avant d’exporter les données.", "error");
        return;
    }
    try {
        const blob = await fetchBlob(`/trips/${state.selectedTrip.id}/exports/${fileName}`);
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
};

ctx.renderTrips = function () {
    const list = $("tripList");
    if (!state.trips.length) {
        list.innerHTML = `<div class="list-item"><strong>Aucun voyage</strong><span class="small">Crée un premier voyage.</span></div>`;
        return;
    }
    list.innerHTML = state.trips.map(trip => `
        <div class="list-item ${state.selectedTrip?.id === trip.id ? "active" : ""}" data-trip-id="${trip.id}">
            <strong>${ctx.escapeHtml(trip.name)}</strong>
            <span class="small">${dateFr(trip.startDate)} → ${dateFr(trip.endDate)} · ${trip.referenceCurrency}</span>
        </div>
    `).join("");
    list.querySelectorAll("[data-trip-id]").forEach(item => {
        item.addEventListener("click", () => ctx.selectTrip(item.dataset.tripId));
    });
};

ctx.renderSelectedTrip = function () {
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
    ctx.updatePresencePeriodDateBounds();
};

ctx.renderDashboard = function () {
    const activePersons = state.persons.filter(person => person.active).length;
    const totalExpenses = state.expenses.reduce((sum, expense) => sum + Number(expense.totalAmount || 0) * Number(expense.exchangeRateToTripCurrency || 1), 0);
    if ($("metricPersons")) $("metricPersons").textContent = String(activePersons);
    if ($("metricExpenses")) $("metricExpenses").textContent = String(state.expenses.length);
    if ($("metricTotalExpenses")) $("metricTotalExpenses").textContent = money(totalExpenses, selectedCurrency());
    if ($("metricCurrency")) $("metricCurrency").textContent = selectedCurrency();
};

ctx.renderSettings = function () {
    if (!state.selectedTrip) {
        if ($("settingsTripName")) $("settingsTripName").textContent = "—";
        if ($("settingsTripDates")) $("settingsTripDates").textContent = "—";
        if ($("settingsTripCurrency")) $("settingsTripCurrency").textContent = "—";
        return;
    }
    if ($("settingsTripName")) $("settingsTripName").textContent = state.selectedTrip.name;
    if ($("settingsTripDates")) $("settingsTripDates").textContent = `${dateFr(state.selectedTrip.startDate)} → ${dateFr(state.selectedTrip.endDate)}`;
    if ($("settingsTripCurrency")) $("settingsTripCurrency").textContent = state.selectedTrip.referenceCurrency || "EUR";
};
}
