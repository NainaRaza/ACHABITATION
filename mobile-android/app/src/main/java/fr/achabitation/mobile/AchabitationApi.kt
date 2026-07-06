package fr.achabitation.mobile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String, val status: Int? = null) : Exception(message)

class AchabitationApi(
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String?
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    suspend fun register(request: RegisterRequest): AuthResponse = post("/auth/register", request, AuthResponse.serializer(), authRequired = false)
    suspend fun login(request: LoginRequest): AuthResponse = post("/auth/login", request, AuthResponse.serializer(), authRequired = false)
    suspend fun profile(): UserProfileResponse = get("/auth/profile", UserProfileResponse.serializer())
    suspend fun updateAccount(request: AccountUpdateRequest): AuthResponse = put("/auth/account", request, AuthResponse.serializer())
    suspend fun updateProfile(request: UserProfileRequest): UserProfileResponse = put("/auth/profile", request, UserProfileResponse.serializer())
    suspend fun applyProfileToLinkedPersons(request: ApplyProfileToLinkedPersonsRequest): UserProfileResponse = post("/auth/profile/apply-to-linked-persons", request, UserProfileResponse.serializer())

    suspend fun trips(): List<TripResponse> = getList("/trips", TripResponse.serializer())
    suspend fun createTrip(request: TripCreateRequest): TripResponse = post("/trips", request, TripResponse.serializer())
    suspend fun updateTripConstraints(tripId: String, request: TripConstraintUpdateRequest): TripResponse = put("/trips/$tripId/constraints", request, TripResponse.serializer())
    suspend fun joinTrip(tripId: String, request: JoinTripRequest): TripResponse = post("/trips/$tripId/join", request, TripResponse.serializer())
    suspend fun joinTripByCode(request: JoinTripByCodeRequest): TripResponse = post("/trips/join-by-code", request, TripResponse.serializer())
    suspend fun invitations(tripId: String): List<TripInvitationResponse> = getList("/trips/$tripId/invitations", TripInvitationResponse.serializer())
    suspend fun createInvitation(tripId: String, request: TripInvitationCreateRequest): TripInvitationResponse = post("/trips/$tripId/invitations", request, TripInvitationResponse.serializer())
    suspend fun revokeInvitation(tripId: String, invitationId: String) = delete("/trips/$tripId/invitations/$invitationId")

    suspend fun persons(tripId: String): List<PersonResponse> = getList("/trips/$tripId/persons", PersonResponse.serializer())
    suspend fun createPerson(tripId: String, request: PersonCreateRequest): PersonResponse = post("/trips/$tripId/persons", request, PersonResponse.serializer())
    suspend fun updatePerson(tripId: String, personId: String, request: PersonUpdateRequest): PersonResponse = put("/trips/$tripId/persons/$personId", request, PersonResponse.serializer())
    suspend fun createCurrentUserPerson(tripId: String, request: CurrentUserPersonCreateRequest): PersonResponse = post("/trips/$tripId/persons/current-user", request, PersonResponse.serializer())
    suspend fun linkCurrentUser(tripId: String, personId: String, request: LinkGuestRequest): PersonResponse = post("/trips/$tripId/persons/$personId/link-current-user", request, PersonResponse.serializer())
    suspend fun disablePerson(tripId: String, personId: String) = delete("/trips/$tripId/persons/$personId")

    suspend fun expenses(tripId: String): List<ExpenseResponse> = getList("/trips/$tripId/expenses", ExpenseResponse.serializer())
    suspend fun createExpense(tripId: String, request: ExpenseCreateRequest): ExpenseResponse = post("/trips/$tripId/expenses", request, ExpenseResponse.serializer())
    suspend fun updateExpense(tripId: String, expenseId: String, request: ExpenseCreateRequest): ExpenseResponse = put("/trips/$tripId/expenses/$expenseId", request, ExpenseResponse.serializer())
    suspend fun deleteExpense(tripId: String, expenseId: String) = delete("/trips/$tripId/expenses/$expenseId")

    suspend fun summary(tripId: String): SummaryResponse = get("/trips/$tripId/summary", SummaryResponse.serializer())
    suspend fun auditLogs(tripId: String): List<AuditLogResponse> = getList("/trips/$tripId/audit-logs", AuditLogResponse.serializer())
    suspend fun exportExpensesCsv(tripId: String): String = getText("/trips/$tripId/exports/expenses.csv")
    suspend fun exportSummaryCsv(tripId: String): String = getText("/trips/$tripId/exports/summary.csv")

    private suspend fun <T> getList(path: String, itemSerializer: KSerializer<T>): List<T> =
        request(path, "GET", null, ListSerializer(itemSerializer), authRequired = true)

    private suspend fun <T> get(path: String, serializer: KSerializer<T>): T =
        request(path, "GET", null, serializer, authRequired = true)

    private suspend fun <Req, Res> post(path: String, body: Req, responseSerializer: KSerializer<Res>, authRequired: Boolean = true): Res =
        request(path, "POST", json.encodeToString(serializerFor(body), body), responseSerializer, authRequired)

    private suspend fun <Req, Res> put(path: String, body: Req, responseSerializer: KSerializer<Res>, authRequired: Boolean = true): Res =
        request(path, "PUT", json.encodeToString(serializerFor(body), body), responseSerializer, authRequired)

    private suspend fun delete(path: String) = requestUnit(path, "DELETE", null, authRequired = true)

    @Suppress("UNCHECKED_CAST")
    private fun <Req> serializerFor(body: Req): KSerializer<Req> = when (body) {
        is RegisterRequest -> RegisterRequest.serializer()
        is LoginRequest -> LoginRequest.serializer()
        is AccountUpdateRequest -> AccountUpdateRequest.serializer()
        is UserProfileRequest -> UserProfileRequest.serializer()
        is ApplyProfileToLinkedPersonsRequest -> ApplyProfileToLinkedPersonsRequest.serializer()
        is TripCreateRequest -> TripCreateRequest.serializer()
        is TripConstraintUpdateRequest -> TripConstraintUpdateRequest.serializer()
        is JoinTripRequest -> JoinTripRequest.serializer()
        is JoinTripByCodeRequest -> JoinTripByCodeRequest.serializer()
        is TripInvitationCreateRequest -> TripInvitationCreateRequest.serializer()
        is PersonCreateRequest -> PersonCreateRequest.serializer()
        is PersonUpdateRequest -> PersonUpdateRequest.serializer()
        is CurrentUserPersonCreateRequest -> CurrentUserPersonCreateRequest.serializer()
        is LinkGuestRequest -> LinkGuestRequest.serializer()
        is ExpenseCreateRequest -> ExpenseCreateRequest.serializer()
        else -> error("Unsupported request type: ${body!!::class.java.name}")
    } as KSerializer<Req>

    private suspend fun <T> request(path: String, method: String, jsonBody: String?, responseSerializer: KSerializer<T>, authRequired: Boolean): T =
        withContext(Dispatchers.IO) {
            val payload = rawRequest(path, method, jsonBody, authRequired, accept = "application/json")
            if (payload.isBlank()) throw ApiException("Réponse vide de l’API.")
            json.decodeFromString(responseSerializer, payload)
        }

    private suspend fun requestUnit(path: String, method: String, jsonBody: String?, authRequired: Boolean) = withContext(Dispatchers.IO) {
        rawRequest(path, method, jsonBody, authRequired, accept = "application/json")
        Unit
    }

    private suspend fun getText(path: String): String = withContext(Dispatchers.IO) {
        rawRequest(path, "GET", null, authRequired = true, accept = "text/csv, text/plain, application/json")
    }

    private fun rawRequest(path: String, method: String, jsonBody: String?, authRequired: Boolean, accept: String): String {
        val baseUrl = baseUrlProvider().trimEnd('/')
        val url = URL(baseUrl + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", accept)
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val token = tokenProvider()
            if (authRequired && token.isNullOrBlank()) throw ApiException("Session absente. Reconnecte-toi.", 401)
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            if (jsonBody != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer -> writer.write(jsonBody) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ApiException(parseError(payload, status), status)
            return payload
        } finally {
            connection.disconnect()
        }
    }

    private fun parseError(payload: String, status: Int): String {
        if (payload.isBlank()) return "Erreur HTTP $status"
        return try {
            val error = json.decodeFromString(ApiErrorResponse.serializer(), payload)
            val details = error.details.joinToString(" · ")
            listOfNotNull(error.error, details.ifBlank { null }).joinToString(" : ").ifBlank { "Erreur HTTP $status" }
        } catch (_: Exception) {
            payload.take(300)
        }
    }
}
