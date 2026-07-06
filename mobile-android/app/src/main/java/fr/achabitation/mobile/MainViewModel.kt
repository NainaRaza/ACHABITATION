package fr.achabitation.mobile

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AppUiState(
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val baseUrl: String = "http://10.0.2.2:8080/api/v1",
    val auth: AuthResponse? = null,
    val profile: UserProfileResponse? = null,
    val trips: List<TripResponse> = emptyList(),
    val selectedTrip: TripResponse? = null,
    val persons: List<PersonResponse> = emptyList(),
    val expenses: List<ExpenseResponse> = emptyList(),
    val summary: SummaryResponse? = null,
    val invitations: List<TripInvitationResponse> = emptyList(),
    val auditLogs: List<AuditLogResponse> = emptyList(),
    val exportPreview: ExportPreview? = null,
    val selectedTab: TripTab = TripTab.Overview
)

data class ExportPreview(
    val title: String,
    val content: String
)

enum class TripTab(val label: String) {
    Overview("Vue"),
    Persons("Personnes"),
    Expenses("Dépenses"),
    Summary("Résumé"),
    Invitations("Invitations"),
    Audit("Audit")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("achabitation-mobile", Context.MODE_PRIVATE)

    var state by mutableStateOf(
        AppUiState(
            baseUrl = prefs.getString("baseUrl", "http://10.0.2.2:8080/api/v1") ?: "http://10.0.2.2:8080/api/v1",
            auth = readAuth()
        )
    )
        private set

    private val api = AchabitationApi(
        baseUrlProvider = { state.baseUrl },
        tokenProvider = { state.auth?.devToken }
    )

    init {
        if (state.auth != null) refreshDashboard(silent = true)
    }

    fun updateBaseUrl(value: String) {
        state = state.copy(baseUrl = value)
        prefs.edit().putString("baseUrl", value).apply()
    }

    fun clearMessage() {
        state = state.copy(message = null, error = null)
    }

    fun clearExportPreview() {
        state = state.copy(exportPreview = null)
    }

    fun login(email: String, password: String) = runTask("Connexion réussie.") {
        val auth = api.login(LoginRequest(email.trim(), password))
        saveAuth(auth)
        state = state.copy(auth = auth, selectedTrip = null)
        loadDashboardData()
    }

    fun register(email: String, displayName: String, password: String) = runTask("Compte créé.") {
        val auth = api.register(RegisterRequest(email.trim(), displayName.trim(), password))
        saveAuth(auth)
        state = state.copy(auth = auth, selectedTrip = null)
        loadDashboardData()
    }

    fun updateAccount(email: String, displayName: String) = runTask("Compte modifié.") {
        val auth = api.updateAccount(AccountUpdateRequest(email.trim(), displayName.trim()))
        saveAuth(auth)
        state = state.copy(auth = auth)
        loadProfileOnly()
    }

    fun updateProfile(request: UserProfileRequest) = runTask("Profil RAV modifié.") {
        val profile = api.updateProfile(request)
        state = state.copy(profile = profile)
        refreshTripIfSelected()
    }

    fun applyProfileToLinkedPersons(personIds: Set<String>) = runTask("Profil appliqué aux personnes liées.") {
        if (personIds.isEmpty()) throw IllegalArgumentException("Sélectionne au moins une personne liée.")
        val profile = api.applyProfileToLinkedPersons(ApplyProfileToLinkedPersonsRequest(personIds))
        state = state.copy(profile = profile)
        refreshTripIfSelected()
    }

    fun logout() {
        prefs.edit().remove("userId").remove("email").remove("displayName").remove("devToken").remove("selectedTripId").apply()
        state = AppUiState(baseUrl = state.baseUrl, message = "Déconnexion effectuée.")
    }

    fun refreshDashboard(silent: Boolean = false) = runTask(if (silent) null else "Données synchronisées.") {
        loadDashboardData()
    }

    fun createTrip(name: String, startDate: String, endDate: String, currency: String, constraints: String) = runTask("Voyage créé.") {
        val payload = TripCreateRequest(
            name = name.trim(),
            startDate = startDate.trim(),
            endDate = endDate.trim(),
            referenceCurrency = currency.trim().ifBlank { "EUR" }.uppercase(),
            customConstraints = parseCsvSet(constraints)
        )
        validateTripDates(payload.startDate, payload.endDate)
        val trip = api.createTrip(payload)
        loadDashboardData()
        selectTripInternal(trip, TripTab.Overview)
    }

    fun updateTripConstraints(constraints: String) = runTask("Contraintes du voyage modifiées.") {
        val trip = requireSelectedTrip()
        val updated = api.updateTripConstraints(trip.id, TripConstraintUpdateRequest(parseCsvSet(constraints)))
        val trips = state.trips.map { if (it.id == updated.id) updated else it }
        state = state.copy(selectedTrip = updated, trips = trips)
        loadTripData(updated.id)
    }

    fun joinTripByCode(code: String) = runTask("Voyage rejoint. Choisis maintenant ton guest ou ajoute-toi directement.") {
        val trip = api.joinTripByCode(JoinTripByCodeRequest(invitationCode = code.trim(), applyProfileToGuest = false))
        loadDashboardData()
        selectTripInternal(trip, TripTab.Persons)
    }

    fun selectTrip(trip: TripResponse) = runTask(null) {
        selectTripInternal(trip, TripTab.Overview)
    }

    fun backToTrips() {
        prefs.edit().remove("selectedTripId").apply()
        state = state.copy(
            selectedTrip = null,
            persons = emptyList(),
            expenses = emptyList(),
            summary = null,
            invitations = emptyList(),
            auditLogs = emptyList(),
            exportPreview = null,
            selectedTab = TripTab.Overview
        )
    }

    fun setTab(tab: TripTab) {
        state = state.copy(selectedTab = tab, exportPreview = null)
        when (tab) {
            TripTab.Summary -> if (state.summary == null && state.selectedTrip != null) refreshTripData()
            TripTab.Invitations -> if (state.invitations.isEmpty() && state.selectedTrip != null) loadInvitations()
            TripTab.Audit -> if (state.auditLogs.isEmpty() && state.selectedTrip != null) loadAuditLogs()
            else -> Unit
        }
    }

    fun refreshTripData() = runTask("Voyage synchronisé.") {
        state.selectedTrip?.let { loadTripData(it.id) }
    }

    fun loadInvitations() = runTask(null) {
        val trip = requireSelectedTrip()
        state = state.copy(invitations = api.invitations(trip.id))
    }

    fun createInvitation(role: String, expiresInDays: String) = runTask("Invitation créée.") {
        val trip = requireSelectedTrip()
        val days = expiresInDays.toIntOrNull()?.coerceIn(1, 30) ?: 7
        api.createInvitation(trip.id, TripInvitationCreateRequest(roleToGrant = role, expiresInDays = days))
        state = state.copy(invitations = api.invitations(trip.id), selectedTab = TripTab.Invitations)
    }

    fun revokeInvitation(invitationId: String) = runTask("Invitation révoquée.") {
        val trip = requireSelectedTrip()
        api.revokeInvitation(trip.id, invitationId)
        state = state.copy(invitations = api.invitations(trip.id))
    }

    fun loadAuditLogs() = runTask(null) {
        val trip = requireSelectedTrip()
        state = state.copy(auditLogs = api.auditLogs(trip.id))
    }

    fun exportExpensesCsv() = runTask("Export dépenses chargé.") {
        val trip = requireSelectedTrip()
        state = state.copy(exportPreview = ExportPreview("depenses.csv", api.exportExpensesCsv(trip.id)), selectedTab = TripTab.Summary)
    }

    fun exportSummaryCsv() = runTask("Export résumé chargé.") {
        val trip = requireSelectedTrip()
        state = state.copy(exportPreview = ExportPreview("resume.csv", api.exportSummaryCsv(trip.id)), selectedTab = TripTab.Summary)
    }

    fun createCurrentUserPerson(applyProfile: Boolean) = runTask("Ton compte a été ajouté au voyage.") {
        val trip = requireSelectedTrip()
        api.createCurrentUserPerson(
            trip.id,
            CurrentUserPersonCreateRequest(
                name = state.auth?.displayName ?: state.profile?.displayName ?: "Moi",
                applyProfileToPerson = applyProfile,
                presencePeriods = defaultPresencePeriods(trip)
            )
        )
        loadProfileOnly()
        loadTripData(trip.id)
    }

    fun linkGuest(personId: String, applyProfile: Boolean) = runTask("Guest lié à ton compte.") {
        val trip = requireSelectedTrip()
        api.linkCurrentUser(trip.id, personId, LinkGuestRequest(applyProfileToGuest = applyProfile))
        loadProfileOnly()
        loadTripData(trip.id)
    }

    fun savePerson(personId: String?, request: PersonUpdateRequest) = runTask(if (personId == null) "Personne ajoutée." else "Personne modifiée.") {
        val trip = requireSelectedTrip()
        validatePresence(request.presencePeriods, trip)
        if (personId == null) {
            api.createPerson(trip.id, request.toCreateRequest())
        } else {
            api.updatePerson(trip.id, personId, request)
        }
        loadProfileOnly()
        loadTripData(trip.id)
        state = state.copy(selectedTab = TripTab.Persons)
    }

    fun disablePerson(personId: String) = runTask("Personne désactivée.") {
        val trip = requireSelectedTrip()
        api.disablePerson(trip.id, personId)
        loadTripData(trip.id)
    }

    fun saveExpense(expenseId: String?, request: ExpenseCreateRequest) = runTask(if (expenseId == null) "Dépense ajoutée." else "Dépense modifiée.") {
        val trip = requireSelectedTrip()
        if (request.payerPersonId.isBlank()) throw IllegalArgumentException("Sélectionne un payeur.")
        if (request.totalAmount <= 0.0) throw IllegalArgumentException("Montant invalide.")
        validateDateInsideTrip(request.date, trip, "La date de dépense")
        if (expenseId == null) api.createExpense(trip.id, request) else api.updateExpense(trip.id, expenseId, request)
        loadTripData(trip.id)
        state = state.copy(selectedTab = TripTab.Expenses)
    }

    fun deleteExpense(expenseId: String) = runTask("Dépense supprimée.") {
        val trip = requireSelectedTrip()
        api.deleteExpense(trip.id, expenseId)
        loadTripData(trip.id)
    }

    private suspend fun loadDashboardData() {
        val profile = api.profile()
        val trips = api.trips()
        state = state.copy(profile = profile, trips = trips)
        val selectedId = prefs.getString("selectedTripId", null)
        val selected = trips.firstOrNull { it.id == selectedId }
        if (selected != null) selectTripInternal(selected, state.selectedTab)
    }

    private suspend fun selectTripInternal(trip: TripResponse, tab: TripTab) {
        prefs.edit().putString("selectedTripId", trip.id).apply()
        state = state.copy(selectedTrip = trip, selectedTab = tab, summary = null, invitations = emptyList(), auditLogs = emptyList(), exportPreview = null)
        loadTripData(trip.id)
    }

    private suspend fun loadTripData(tripId: String) {
        val persons = api.persons(tripId)
        val expenses = api.expenses(tripId)
        val summary = runCatching { api.summary(tripId) }.getOrNull()
        val invitations = if (state.selectedTab == TripTab.Invitations) runCatching { api.invitations(tripId) }.getOrDefault(state.invitations) else state.invitations
        val audit = if (state.selectedTab == TripTab.Audit) runCatching { api.auditLogs(tripId) }.getOrDefault(state.auditLogs) else state.auditLogs
        state = state.copy(persons = persons, expenses = expenses, summary = summary, invitations = invitations, auditLogs = audit)
    }

    private suspend fun loadProfileOnly() {
        state = state.copy(profile = api.profile())
    }

    private suspend fun refreshTripIfSelected() {
        val trip = state.selectedTrip ?: return
        loadTripData(trip.id)
    }

    private fun runTask(successMessage: String?, block: suspend () -> Unit) {
        viewModelScope.launch {
            state = state.copy(loading = true, message = null, error = null)
            try {
                block()
                state = state.copy(loading = false, message = successMessage, error = null)
            } catch (ex: Exception) {
                state = state.copy(loading = false, error = ex.message ?: ex::class.java.simpleName, message = null)
            }
        }
    }

    private fun requireSelectedTrip(): TripResponse = state.selectedTrip ?: throw IllegalArgumentException("Sélectionne un voyage.")

    private fun defaultPresencePeriods(trip: TripResponse): List<PresencePeriodRequest> =
        listOf(PresencePeriodRequest(trip.startDate, trip.endDate))

    private fun validateTripDates(start: String, end: String) {
        val s = parseIsoDate(start, "Date de début invalide")
        val e = parseIsoDate(end, "Date de fin invalide")
        if (e.isBefore(s)) throw IllegalArgumentException("La date de fin doit être postérieure à la date de début.")
    }

    private fun validateDateInsideTrip(date: String, trip: TripResponse, label: String) {
        val d = parseIsoDate(date, "$label invalide")
        val start = parseIsoDate(trip.startDate, "Début du voyage invalide")
        val end = parseIsoDate(trip.endDate, "Fin du voyage invalide")
        if (d.isBefore(start) || d.isAfter(end)) throw IllegalArgumentException("$label doit être comprise dans le voyage.")
    }

    private fun validatePresence(periods: List<PresencePeriodRequest>, trip: TripResponse) {
        if (periods.isEmpty()) throw IllegalArgumentException("Ajoute au moins une période de présence.")
        periods.forEach {
            val s = parseIsoDate(it.startDate, "Début de présence invalide")
            val e = parseIsoDate(it.endDate, "Fin de présence invalide")
            if (e.isBefore(s)) throw IllegalArgumentException("Une période de présence est inversée.")
            validateDateInsideTrip(it.startDate, trip, "Le début de présence")
            validateDateInsideTrip(it.endDate, trip, "La fin de présence")
        }
    }

    private fun parseIsoDate(value: String, message: String): LocalDate =
        runCatching { LocalDate.parse(value.trim()) }.getOrElse { throw IllegalArgumentException("$message. Format attendu : AAAA-MM-JJ.") }

    private fun saveAuth(auth: AuthResponse) {
        prefs.edit()
            .putString("userId", auth.userId)
            .putString("email", auth.email)
            .putString("displayName", auth.displayName)
            .putString("devToken", auth.devToken)
            .apply()
    }

    private fun readAuth(): AuthResponse? {
        val userId = prefs.getString("userId", null) ?: return null
        val email = prefs.getString("email", null) ?: return null
        val displayName = prefs.getString("displayName", null) ?: return null
        val token = prefs.getString("devToken", null) ?: return null
        return AuthResponse(userId = userId, email = email, displayName = displayName, devToken = token)
    }
}

fun PersonUpdateRequest.toCreateRequest(): PersonCreateRequest = PersonCreateRequest(
    name = name,
    livingRest = livingRest,
    weightMode = weightMode,
    advancedLivingRest = advancedLivingRest,
    netIncomeAfterTax = netIncomeAfterTax,
    rent = rent,
    credits = credits,
    fixedCharges = fixedCharges,
    transport = transport,
    insurance = insurance,
    otherMandatoryExpenses = otherMandatoryExpenses,
    menstrualProtection = menstrualProtection,
    vegetarian = vegetarian,
    noAlcohol = noAlcohol,
    livingRestPublic = livingRestPublic,
    customConstraints = customConstraints,
    presencePeriods = presencePeriods
)

fun todayIso(): String = LocalDate.now().toString()
fun plusDaysIso(days: Long): String = LocalDate.now().plusDays(days).toString()
fun parseCsvSet(value: String): Set<String> = value.split(',', ';', '\n')
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { it.replaceFirstChar { c -> c.uppercase() } }
    .toSet()
fun Double?.toInput(): String = this?.takeIf { it != 0.0 }?.toString() ?: ""
