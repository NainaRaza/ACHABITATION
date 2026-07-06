// Module fonctionnel extrait de app.js.
// Les fonctions sont installées dans un contexte partagé pour éviter les dépendances circulaires entre modules.

export function install(ctx) {
    const { state, writeJson, asArray, api, fetchBlob, $, showMessage, showApiError, money, dateFr, selectedCurrency, canonicalConstraintName, constraintKey } = ctx;

ctx.init = async function () {
    ctx.bindEvents();
    ctx.hydrateUserUi();
    await ctx.checkBackend();
    if (state.user?.accessToken) {
        await ctx.loadProfile(false);
        await ctx.loadTrips();
        if (state.selectedTrip?.id && state.trips.some(t => t.id === state.selectedTrip.id)) {
            state.selectedTrip = state.trips.find(t => t.id === state.selectedTrip.id);
            await ctx.loadTripData();
        } else if (state.trips.length > 0) {
            ctx.selectTrip(state.trips[0].id);
        } else {
            ctx.renderAll();
        }
    } else {
        ctx.renderAll();
    }
};

ctx.bindEvents = function () {
    $("loginUserBtn")?.addEventListener("click", ctx.loginUser);
    $("createUserBtn")?.addEventListener("click", ctx.createUser);
    $("logoutUserBtn")?.addEventListener("click", ctx.logoutUser);
    $("showAccountEditBtn")?.addEventListener("click", () => ctx.showPanel("accountEditForm"));
    $("cancelAccountEditBtn")?.addEventListener("click", () => {
        ctx.hydrateUserUi();
        ctx.hidePanel("accountEditForm");
    });
    $("accountEditForm")?.addEventListener("submit", ctx.updateAccount);
    ["loginIdentifier", "loginPassword"].forEach(id => $(id)?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            ctx.loginUser();
        }
    }));
    ["registerEmail", "registerDisplayName", "registerPassword"].forEach(id => $(id)?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            ctx.createUser();
        }
    }));
    $("topProfileBtn").addEventListener("click", ctx.openProfilePanel);
    $("closeProfilePanelBtn").addEventListener("click", () => ctx.activateTab("dashboard"));
    $("hideProfileApplyPanelBtn").addEventListener("click", () => ctx.hidePanel("profileApplyPanel"));
    $("applyProfileToLinkedBtn").addEventListener("click", ctx.applyProfileToSelectedLinkedPersons);

    $("showTripFormBtn").addEventListener("click", () => ctx.showPanel("tripFormPanel"));
    $("closeTripFormBtn").addEventListener("click", () => ctx.hidePanel("tripFormPanel"));
    $("tripForm").addEventListener("submit", ctx.createTrip);
    $("joinTripByCodeBtn")?.addEventListener("click", ctx.joinTripByInvitationCode);
    $("dismissPostJoinClaimBtn")?.addEventListener("click", () => ctx.hidePanel("postJoinClaimPanel"));
    $("postJoinCreateSelfBtn")?.addEventListener("click", () => ctx.createCurrentUserPerson());
    $("joinInvitationCode")?.addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            ctx.joinTripByInvitationCode();
        }
    });

    $("profileForm").addEventListener("submit", ctx.saveProfile);
    $("profileWeightMode").addEventListener("change", ctx.updateProfileFieldState);
    $("profileAdvancedRav").addEventListener("change", ctx.updateProfileFieldState);
    $("addTripConstraintBtn").addEventListener("click", ctx.addTripConstraintFromSidebar);
    $("newTripConstraintName").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            ctx.addTripConstraintFromSidebar();
        }
    });
    $("refreshBtn").addEventListener("click", ctx.refreshCurrent);

    document.querySelectorAll(".tab").forEach(button => {
        button.addEventListener("click", () => ctx.activateTab(button.dataset.tab));
    });

    $("quickAddExpenseBtn").addEventListener("click", () => ctx.openExpenseForm());
    $("quickAddPersonBtn").addEventListener("click", () => ctx.openPersonForm());
    $("dashboardAddExpenseBtn").addEventListener("click", () => ctx.openExpenseForm());
    $("dashboardAddPersonBtn").addEventListener("click", () => ctx.openPersonForm());
    $("dashboardSummaryBtn").addEventListener("click", () => ctx.activateTab("summary"));

    $("addCurrentUserPersonBtn")?.addEventListener("click", () => ctx.createCurrentUserPerson());
    $("showPersonFormBtn").addEventListener("click", () => ctx.openPersonForm());
    $("closePersonFormBtn").addEventListener("click", () => ctx.hidePanel("personFormPanel"));
    $("personForm").addEventListener("submit", ctx.savePerson);
    $("resetPersonBtn").addEventListener("click", event => {
        event.preventDefault();
        ctx.resetPersonForm();
        ctx.showPanel("personFormPanel");
    });
    $("personWeightMode").addEventListener("change", ctx.updatePersonFieldState);
    $("personAdvancedRav").addEventListener("change", ctx.updatePersonFieldState);
    $("calculateRavBtn").addEventListener("click", ctx.calculateAdvancedRav);
    $("addCustomConstraintBtn").addEventListener("click", ctx.addCustomConstraintFromForm);
    $("newCustomConstraintName").addEventListener("keydown", event => {
        if (event.key === "Enter") {
            event.preventDefault();
            ctx.addCustomConstraintFromForm();
        }
    });
    $("addPresencePeriodBtn").addEventListener("click", ctx.addPresencePeriodRow);
    $("presencePeriodsRows").addEventListener("click", event => {
        if (event.target.classList.contains("remove-presence-period")) ctx.removePresencePeriodRow(event.target);
    });
    $("presencePeriodsRows").addEventListener("change", ctx.updatePresencePeriodDateBounds);

    $("showExpenseFormBtn").addEventListener("click", () => ctx.openExpenseForm());
    $("closeExpenseFormBtn").addEventListener("click", () => ctx.hidePanel("expenseFormPanel"));
    $("expenseForm").addEventListener("submit", ctx.saveExpense);
    $("resetExpenseBtn").addEventListener("click", event => {
        event.preventDefault();
        ctx.resetExpenseForm();
        ctx.showPanel("expenseFormPanel");
    });
    $("expenseAdvancedMode").addEventListener("change", ctx.renderManualParticipants);
    $("expenseType").addEventListener("change", ctx.updateExpenseFieldState);

    $("reloadSummaryBtn").addEventListener("click", ctx.loadSummary);
    $("exportExpensesCsvBtn")?.addEventListener("click", () => ctx.downloadExport("expenses.csv"));
    $("exportSummaryCsvBtn")?.addEventListener("click", () => ctx.downloadExport("summary.csv"));
    $("reloadAuditBtn").addEventListener("click", ctx.loadAuditLogs);
    $("createInviteBtn")?.addEventListener("click", ctx.createInvitation);

    const defaultDate = state.selectedTrip?.startDate && ctx.isBeforeIso(ctx.todayIso(), state.selectedTrip.startDate)
        ? state.selectedTrip.startDate
        : state.selectedTrip?.endDate && ctx.isAfterIso(ctx.todayIso(), state.selectedTrip.endDate)
            ? state.selectedTrip.endDate
            : ctx.todayIso();
    $("expenseDate").value = defaultDate;
};

ctx.activateTab = function (tab) {
    document.querySelectorAll(".tab").forEach(b => b.classList.toggle("active", b.dataset.tab === tab));
    document.querySelectorAll(".tab-panel").forEach(panel => panel.classList.remove("active"));
    $(`${tab}Tab`).classList.add("active");
    if (tab === "summary") ctx.loadSummary();
    if (tab === "audit") ctx.loadAuditLogs();
};

ctx.renderAll = function () {
    ctx.renderTrips();
    ctx.renderSelectedTrip();
    ctx.renderDashboard();
    ctx.renderPostJoinClaimPanel();
    ctx.renderSettings();
    ctx.renderPersons();
    ctx.renderTripConstraints();
    ctx.renderCustomConstraintCheckboxes();
    ctx.renderProfile();
    ctx.renderExpensePersonOptions();
    ctx.renderExpenseCustomAmountRows();
    ctx.renderExpenses();
    ctx.renderSummary();
    ctx.renderAuditLogs();
    ctx.renderInvitations();
    if (!document.querySelector(".presence-period-row")) ctx.renderPresencePeriodRows();
    ctx.updatePresencePeriodDateBounds();
    ctx.updatePersonFieldState();
    ctx.updateExpenseFieldState();
};
}
