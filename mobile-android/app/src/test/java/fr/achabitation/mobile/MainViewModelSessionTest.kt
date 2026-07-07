package fr.achabitation.mobile

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MainViewModelSessionTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var storage: FakeSessionStorage
    private lateinit var api: FakeAchabitationClient

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        storage = FakeSessionStorage()
        api = FakeAchabitationClient()
    }

    @After
    fun tearDown() {
        storage.clear()
    }

    @Test
    fun login_success_stores_token_and_loads_authenticated_state() = runTest {
        val viewModel = newViewModel()

        viewModel.login(" user@example.test ", "secret")
        advanceUntilIdle()

        assertEquals("Connexion réussie.", viewModel.state.message)
        assertEquals("token-ok", viewModel.state.auth?.accessToken)
        assertEquals("token-ok", storage.getString("accessToken"))
        assertEquals("user@example.test", api.lastLoginEmail)
        assertNotNull(viewModel.state.profile)
        assertFalse(viewModel.state.loading)
    }

    @Test
    fun login_failure_does_not_store_token_and_exposes_clean_error() = runTest {
        api.loginFailure = ApiException("Identifiants invalides.", 401)
        val viewModel = newViewModel()

        viewModel.login("user@example.test", "bad-password")
        advanceUntilIdle()

        assertNull(viewModel.state.auth)
        assertNull(storage.getString("accessToken"))
        assertFalse(viewModel.state.sessionExpired)
        assertEquals("Identifiants invalides.", viewModel.state.error)
        assertFalse(viewModel.state.loading)
    }

    @Test
    fun logout_success_calls_backend_and_clears_local_session() = runTest {
        storage.seedAuth()
        val viewModel = newViewModel()

        viewModel.logout()
        advanceUntilIdle()

        assertTrue(api.logoutCalled)
        assertNull(viewModel.state.auth)
        assertNull(storage.getString("accessToken"))
        assertEquals("Déconnexion effectuée.", viewModel.state.message)
    }

    @Test
    fun logout_401_clears_local_session() = runTest {
        storage.seedAuth()
        api.logoutFailure = ApiException("Unauthorized", 401)
        val viewModel = newViewModel()

        viewModel.logout()
        advanceUntilIdle()

        assertTrue(api.logoutCalled)
        assertNull(viewModel.state.auth)
        assertNull(storage.getString("accessToken"))
        assertEquals("Session déjà expirée. Déconnexion locale effectuée.", viewModel.state.message)
    }

    @Test
    fun api_401_during_refresh_clears_token_and_sets_session_expired_state() = runTest {
        storage.seedAuth()
        api.profileFailure = ApiException("Unauthorized", 401)
        val viewModel = newViewModel()

        viewModel.refreshDashboard()
        advanceUntilIdle()

        assertNull(viewModel.state.auth)
        assertNull(storage.getString("accessToken"))
        assertTrue(viewModel.state.sessionExpired)
        assertEquals("Session expirée. Reconnecte-toi.", viewModel.state.error)
    }

    @Test
    fun create_trip_success_selects_trip_and_loads_trip_data() = runTest {
        val viewModel = newViewModel()

        viewModel.createTrip(
            name = " Vacances métier ",
            startDate = "2026-08-01",
            endDate = "2026-08-10",
            currency = " eur ",
            constraints = "Sans porc; Sans lactose"
        )
        advanceUntilIdle()

        assertEquals("Voyage créé.", viewModel.state.message)
        assertEquals("Vacances métier", viewModel.state.selectedTrip?.name)
        assertEquals("EUR", api.lastTripCreateRequest?.referenceCurrency)
        assertEquals(setOf("Sans porc", "Sans lactose"), api.lastTripCreateRequest?.customConstraints)
        assertFalse(viewModel.state.loading)
    }

    @Test
    fun create_trip_invalid_dates_does_not_call_api() = runTest {
        val viewModel = newViewModel()

        viewModel.createTrip(
            name = "Voyage invalide",
            startDate = "2026-08-10",
            endDate = "2026-08-01",
            currency = "EUR",
            constraints = ""
        )
        advanceUntilIdle()

        assertNull(api.lastTripCreateRequest)
        assertEquals("La date de fin doit être postérieure à la date de début.", viewModel.state.error)
    }

    @Test
    fun save_person_success_creates_person_and_refreshes_trip_state() = runTest {
        val viewModel = newViewModel()
        viewModel.createTrip("Voyage", "2026-08-01", "2026-08-10", "EUR", "")
        advanceUntilIdle()

        viewModel.savePerson(null, personRequest(name = "Sofia", livingRest = 1200.0))
        advanceUntilIdle()

        assertEquals("Personne ajoutée.", viewModel.state.message)
        assertEquals("Sofia", api.lastPersonCreateRequest?.name)
        assertEquals(1, viewModel.state.persons.size)
        assertEquals(TripTab.Persons, viewModel.state.selectedTab)
    }

    @Test
    fun save_person_presence_outside_trip_is_rejected_before_api_call() = runTest {
        val viewModel = newViewModel()
        viewModel.createTrip("Voyage", "2026-08-01", "2026-08-10", "EUR", "")
        advanceUntilIdle()

        viewModel.savePerson(
            null,
            personRequest(
                name = "Hors période",
                livingRest = 1200.0,
                periods = listOf(PresencePeriodRequest("2026-07-01", "2026-07-02"))
            )
        )
        advanceUntilIdle()

        assertNull(api.lastPersonCreateRequest)
        assertTrue(viewModel.state.error?.contains("doit être comprise dans le voyage") == true)
    }

    @Test
    fun save_expense_success_creates_expense_and_refreshes_summary() = runTest {
        val viewModel = newViewModel()
        viewModel.createTrip("Voyage", "2026-08-01", "2026-08-10", "EUR", "")
        advanceUntilIdle()
        viewModel.savePerson(null, personRequest(name = "Sofia", livingRest = 1200.0))
        advanceUntilIdle()

        viewModel.saveExpense(
            null,
            ExpenseCreateRequest(
                title = "Courses",
                date = "2026-08-02",
                payerPersonId = "person-1",
                totalAmount = 42.0
            )
        )
        advanceUntilIdle()

        assertEquals("Dépense ajoutée.", viewModel.state.message)
        assertEquals("Courses", api.lastExpenseCreateRequest?.title)
        assertEquals(1, viewModel.state.expenses.size)
        assertNotNull(viewModel.state.summary)
        assertEquals(TripTab.Expenses, viewModel.state.selectedTab)
    }

    @Test
    fun save_expense_invalid_amount_is_rejected_before_api_call() = runTest {
        val viewModel = newViewModel()
        viewModel.createTrip("Voyage", "2026-08-01", "2026-08-10", "EUR", "")
        advanceUntilIdle()

        viewModel.saveExpense(
            null,
            ExpenseCreateRequest(
                title = "Montant invalide",
                date = "2026-08-02",
                payerPersonId = "person-1",
                totalAmount = 0.0
            )
        )
        advanceUntilIdle()

        assertNull(api.lastExpenseCreateRequest)
        assertEquals("Montant invalide.", viewModel.state.error)
    }

    private fun personRequest(
        name: String,
        livingRest: Double,
        periods: List<PresencePeriodRequest> = listOf(PresencePeriodRequest("2026-08-01", "2026-08-10"))
    ) = PersonUpdateRequest(
        name = name,
        livingRest = livingRest,
        weightMode = "LIVING_REST",
        advancedLivingRest = false,
        vegetarian = false,
        noAlcohol = false,
        livingRestPublic = true,
        presencePeriods = periods
    )

    private fun newViewModel(): MainViewModel = MainViewModel(
        application = application,
        storage = storage,
        apiClient = api,
        autoRefresh = false
    )
}

private class FakeSessionStorage : SessionStorage {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String, defaultValue: String?): String? = values[key] ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun remove(vararg keys: String) {
        keys.forEach { values.remove(it) }
    }

    fun clear() = values.clear()

    fun seedAuth() {
        putString("userId", "user-1")
        putString("email", "user@example.test")
        putString("displayName", "User Test")
        putString("accessToken", "stored-token")
    }
}

private class FakeAchabitationClient : AchabitationClient {
    var lastLoginEmail: String? = null
    var logoutCalled: Boolean = false
    var loginFailure: ApiException? = null
    var logoutFailure: ApiException? = null
    var profileFailure: ApiException? = null
    var lastTripCreateRequest: TripCreateRequest? = null
    var lastPersonCreateRequest: PersonCreateRequest? = null
    var lastExpenseCreateRequest: ExpenseCreateRequest? = null
    private val trips = mutableListOf<TripResponse>()
    private val persons = mutableListOf<PersonResponse>()
    private val expenses = mutableListOf<ExpenseResponse>()

    override suspend fun login(request: LoginRequest): AuthResponse {
        lastLoginEmail = request.email
        loginFailure?.let { throw it }
        return auth()
    }

    override suspend fun register(request: RegisterRequest): AuthResponse = auth()

    override suspend fun logout() {
        logoutCalled = true
        logoutFailure?.let { throw it }
    }

    override suspend fun profile(): UserProfileResponse {
        profileFailure?.let { throw it }
        return UserProfileResponse(userId = "user-1", email = "user@example.test", displayName = "User Test")
    }

    override suspend fun updateAccount(request: AccountUpdateRequest): AuthResponse = auth(email = request.email, displayName = request.displayName)
    override suspend fun updateProfile(request: UserProfileRequest): UserProfileResponse = profileResponse()
    override suspend fun applyProfileToLinkedPersons(request: ApplyProfileToLinkedPersonsRequest): UserProfileResponse = profileResponse()
    override suspend fun trips(): List<TripResponse> = trips
    override suspend fun createTrip(request: TripCreateRequest): TripResponse {
        lastTripCreateRequest = request
        val created = trip(name = request.name, startDate = request.startDate, endDate = request.endDate)
        trips.add(created)
        return created
    }
    override suspend fun updateTripConstraints(tripId: String, request: TripConstraintUpdateRequest): TripResponse = trip(id = tripId)
    override suspend fun joinTripByCode(request: JoinTripByCodeRequest): TripResponse = trip()
    override suspend fun invitations(tripId: String): List<TripInvitationResponse> = emptyList()
    override suspend fun createInvitation(tripId: String, request: TripInvitationCreateRequest): TripInvitationResponse = TripInvitationResponse(id = "inv-1", tripId = tripId, code = "ABC123")
    override suspend fun revokeInvitation(tripId: String, invitationId: String) = Unit
    override suspend fun persons(tripId: String): List<PersonResponse> = persons
    override suspend fun createPerson(tripId: String, request: PersonCreateRequest): PersonResponse {
        lastPersonCreateRequest = request
        val created = person(name = request.name)
        persons.add(created)
        return created
    }
    override suspend fun updatePerson(tripId: String, personId: String, request: PersonUpdateRequest): PersonResponse = person(id = personId)
    override suspend fun createCurrentUserPerson(tripId: String, request: CurrentUserPersonCreateRequest): PersonResponse = person()
    override suspend fun linkCurrentUser(tripId: String, personId: String, request: LinkGuestRequest): PersonResponse = person(id = personId)
    override suspend fun disablePerson(tripId: String, personId: String) = Unit
    override suspend fun expenses(tripId: String): List<ExpenseResponse> = expenses
    override suspend fun createExpense(tripId: String, request: ExpenseCreateRequest): ExpenseResponse {
        lastExpenseCreateRequest = request
        val created = expense(title = request.title)
        expenses.add(created)
        return created
    }
    override suspend fun updateExpense(tripId: String, expenseId: String, request: ExpenseCreateRequest): ExpenseResponse = expense(id = expenseId)
    override suspend fun deleteExpense(tripId: String, expenseId: String) = Unit
    override suspend fun summary(tripId: String): SummaryResponse = SummaryResponse()
    override suspend fun auditLogs(tripId: String): List<AuditLogResponse> = emptyList()
    override suspend fun exportExpensesCsv(tripId: String): String = "title,totalAmount\n"
    override suspend fun exportSummaryCsv(tripId: String): String = "from,to,amount\n"

    private fun auth(email: String = "user@example.test", displayName: String = "User Test") = AuthResponse(
        userId = "user-1",
        email = email,
        displayName = displayName,
        accessToken = "token-ok"
    )

    private fun profileResponse() = UserProfileResponse(userId = "user-1", email = "user@example.test", displayName = "User Test")

    private fun trip(
        id: String = "trip-1",
        name: String = "Voyage test",
        startDate: String = "2026-07-01",
        endDate: String = "2026-07-10"
    ) = TripResponse(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate
    )

    private fun person(id: String = "person-1", name: String = "Personne test") = PersonResponse(
        id = id,
        name = name,
        presencePeriods = listOf(PresencePeriodResponse("2026-07-01", "2026-07-10"))
    )

    private fun expense(id: String = "expense-1", title: String = "Dépense test") = ExpenseResponse(
        id = id,
        title = title,
        date = "2026-07-02",
        payerPersonId = "person-1",
        totalAmount = 42.0
    )
}
