package fr.achabitation.mobile

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val status: Int? = null,
    val error: String? = null,
    val details: List<String> = emptyList()
)

@Serializable
data class AuthResponse(
    val userId: String,
    val email: String,
    val displayName: String,
    val accessToken: String,
    val note: String? = null
)

@Serializable
data class RegisterRequest(
    val email: String,
    val displayName: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AccountUpdateRequest(
    val email: String,
    val displayName: String
)

@Serializable
data class UserProfileRequest(
    val displayName: String? = null,
    val livingRest: Double? = null,
    val weightMode: String = "AVERAGE",
    val advancedLivingRest: Boolean = false,
    val netIncomeAfterTax: Double? = null,
    val rent: Double? = null,
    val credits: Double? = null,
    val fixedCharges: Double? = null,
    val transport: Double? = null,
    val insurance: Double? = null,
    val otherMandatoryExpenses: Double? = null,
    val menstrualProtection: Double? = null,
    val vegetarian: Boolean = false,
    val noAlcohol: Boolean = false,
    val livingRestPublic: Boolean = true,
    val customConstraints: Set<String> = emptySet()
)

@Serializable
data class ApplyProfileToLinkedPersonsRequest(
    val personIds: Set<String> = emptySet()
)

@Serializable
data class UserProfileResponse(
    val userId: String,
    val email: String,
    val displayName: String,
    val livingRest: Double? = null,
    val weightMode: String? = null,
    val advancedLivingRest: Boolean = false,
    val netIncomeAfterTax: Double? = null,
    val rent: Double? = null,
    val credits: Double? = null,
    val fixedCharges: Double? = null,
    val transport: Double? = null,
    val insurance: Double? = null,
    val otherMandatoryExpenses: Double? = null,
    val menstrualProtection: Double? = null,
    val vegetarian: Boolean = false,
    val noAlcohol: Boolean = false,
    val livingRestPublic: Boolean = true,
    val knownCustomConstraints: Set<String> = emptySet(),
    val customConstraints: Set<String> = emptySet(),
    val linkedPersons: List<LinkedProfilePersonResponse> = emptyList()
)

@Serializable
data class LinkedProfilePersonResponse(
    val personId: String,
    val personName: String,
    val tripId: String,
    val tripName: String
)

@Serializable
data class TripResponse(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val referenceCurrency: String = "EUR",
    val customConstraints: Set<String> = emptySet(),
    val active: Boolean = true
)

@Serializable
data class TripCreateRequest(
    val name: String,
    val startDate: String,
    val endDate: String,
    val referenceCurrency: String = "EUR",
    val ownerUserId: String? = null,
    val customConstraints: Set<String> = emptySet()
)

@Serializable
data class TripConstraintUpdateRequest(
    val customConstraints: Set<String> = emptySet()
)

@Serializable
data class JoinTripRequest(
    val guestPersonId: String? = null,
    val applyProfileToGuest: Boolean = false,
    val invitationCode: String? = null
)

@Serializable
data class JoinTripByCodeRequest(
    val guestPersonId: String? = null,
    val applyProfileToGuest: Boolean = false,
    val invitationCode: String
)

@Serializable
data class TripInvitationCreateRequest(
    val roleToGrant: String = "PARTICIPANT",
    val expiresInDays: Int? = 7
)

@Serializable
data class TripInvitationResponse(
    val id: String,
    val tripId: String,
    val code: String,
    val roleToGrant: String = "PARTICIPANT",
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val revoked: Boolean = false,
    val usable: Boolean = true
)

@Serializable
data class PresencePeriodRequest(
    val startDate: String,
    val endDate: String
)

@Serializable
data class PresencePeriodResponse(
    val startDate: String,
    val endDate: String
)

@Serializable
data class PersonCreateRequest(
    val name: String,
    val livingRest: Double? = null,
    val weightMode: String = "AVERAGE",
    val advancedLivingRest: Boolean = false,
    val netIncomeAfterTax: Double? = null,
    val rent: Double? = null,
    val credits: Double? = null,
    val fixedCharges: Double? = null,
    val transport: Double? = null,
    val insurance: Double? = null,
    val otherMandatoryExpenses: Double? = null,
    val menstrualProtection: Double? = null,
    val vegetarian: Boolean = false,
    val noAlcohol: Boolean = false,
    val livingRestPublic: Boolean = true,
    val customConstraints: Set<String> = emptySet(),
    val presencePeriods: List<PresencePeriodRequest>
)

@Serializable
data class PersonUpdateRequest(
    val name: String,
    val livingRest: Double? = null,
    val weightMode: String = "AVERAGE",
    val advancedLivingRest: Boolean = false,
    val netIncomeAfterTax: Double? = null,
    val rent: Double? = null,
    val credits: Double? = null,
    val fixedCharges: Double? = null,
    val transport: Double? = null,
    val insurance: Double? = null,
    val otherMandatoryExpenses: Double? = null,
    val menstrualProtection: Double? = null,
    val vegetarian: Boolean = false,
    val noAlcohol: Boolean = false,
    val livingRestPublic: Boolean = true,
    val customConstraints: Set<String> = emptySet(),
    val active: Boolean = true,
    val presencePeriods: List<PresencePeriodRequest>
)

@Serializable
data class CurrentUserPersonCreateRequest(
    val name: String? = null,
    val applyProfileToPerson: Boolean = false,
    val presencePeriods: List<PresencePeriodRequest>
)

@Serializable
data class LinkGuestRequest(
    val userId: String? = null,
    val applyProfileToGuest: Boolean = false
)

@Serializable
data class PersonResponse(
    val id: String,
    val name: String,
    val linkedUserId: String? = null,
    val linkedUserEmail: String? = null,
    val guest: Boolean = true,
    val livingRest: Double? = null,
    val livingRestHidden: Boolean = false,
    val livingRestPublic: Boolean = true,
    val canEditFinancialProfile: Boolean = true,
    val weightMode: String? = null,
    val advancedLivingRest: Boolean = false,
    val netIncomeAfterTax: Double? = null,
    val rent: Double? = null,
    val credits: Double? = null,
    val fixedCharges: Double? = null,
    val transport: Double? = null,
    val insurance: Double? = null,
    val otherMandatoryExpenses: Double? = null,
    val menstrualProtection: Double? = null,
    val vegetarian: Boolean = false,
    val noAlcohol: Boolean = false,
    val customConstraints: Set<String> = emptySet(),
    val active: Boolean = true,
    val presencePeriods: List<PresencePeriodResponse> = emptyList()
)

@Serializable
data class ExpenseCreateRequest(
    val title: String,
    val date: String,
    val payerPersonId: String,
    val totalAmount: Double,
    val meatAmount: Double = 0.0,
    val alcoholAmount: Double = 0.0,
    val customConstraintAmounts: Map<String, Double> = emptyMap(),
    val type: String = "NORMAL",
    val advancedMode: Boolean = false,
    val manualParticipantIds: Set<String> = emptySet(),
    val currency: String = "EUR",
    val exchangeRateToTripCurrency: Double = 1.0
)

@Serializable
data class ExpenseResponse(
    val id: String,
    val title: String,
    val date: String,
    val payerPersonId: String,
    val payerName: String? = null,
    val totalAmount: Double,
    val meatAmount: Double = 0.0,
    val alcoholAmount: Double = 0.0,
    val customConstraintAmounts: Map<String, Double> = emptyMap(),
    val type: String = "NORMAL",
    val advancedMode: Boolean = false,
    val manualParticipantIds: Set<String> = emptySet(),
    val currency: String = "EUR",
    val exchangeRateToTripCurrency: Double = 1.0
)

@Serializable
data class SummaryResponse(
    val referenceCurrency: String = "EUR",
    val balances: List<BalanceResponse> = emptyList(),
    val settlements: List<SettlementResponse> = emptyList()
)

@Serializable
data class BalanceResponse(
    val personId: String,
    val personName: String,
    val totalPaid: Double,
    val totalOwed: Double,
    val balance: Double
)

@Serializable
data class SettlementResponse(
    val fromPersonId: String,
    val fromPersonName: String,
    val toPersonId: String,
    val toPersonName: String,
    val amount: Double
)

@Serializable
data class AuditLogResponse(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val description: String,
    val actorUserId: String? = null,
    val createdAt: String? = null
)
