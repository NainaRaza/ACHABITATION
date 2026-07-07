package fr.achabitation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import fr.achabitation.infrastructure.repository.UserRepository;
import fr.achabitation.infrastructure.repository.PasswordResetTokenRepository;
import fr.achabitation.infrastructure.repository.EmailVerificationTokenRepository;
import fr.achabitation.infrastructure.repository.UserSessionRepository;
import fr.achabitation.application.SessionTokenService;
import fr.achabitation.infrastructure.entity.PasswordResetTokenEntity;
import fr.achabitation.infrastructure.entity.EmailVerificationTokenEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AchabitationApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private SessionTokenService sessionTokenService;

    @Test
    void betaFlowCreatesTripPersonsExpensesSummaryExportsAndAuditLogs() throws Exception {
        AuthSession owner = registerUser("owner@example.com");
        UUID tripId = createTrip(owner);

        UUID sofiaId = createPerson(owner, tripId, """
                {
                  "name": "Sofia",
                  "livingRest": 2000,
                  "weightMode": "LIVING_REST",
                  "advancedLivingRest": false,
                  "vegetarian": false,
                  "noAlcohol": false,
                  "presencePeriods": [
                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                  ]
                }
                """);
        UUID karimId = createPerson(owner, tripId, """
                {
                  "name": "Karim",
                  "livingRest": 0,
                  "weightMode": "AVERAGE",
                  "advancedLivingRest": false,
                  "vegetarian": true,
                  "noAlcohol": false,
                  "presencePeriods": [
                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                  ]
                }
                """);
        createPerson(owner, tripId, """
                {
                  "name": "Lina",
                  "livingRest": null,
                  "weightMode": "LIVING_REST",
                  "advancedLivingRest": true,
                  "netIncomeAfterTax": 2500,
                  "rent": 800,
                  "credits": 100,
                  "fixedCharges": 250,
                  "transport": 80,
                  "insurance": 70,
                  "otherMandatoryExpenses": 100,
                  "menstrualProtection": 20,
                  "vegetarian": false,
                  "noAlcohol": true,
                  "presencePeriods": [
                    {"startDate": "2026-08-01", "endDate": "2026-08-05"},
                    {"startDate": "2026-08-10", "endDate": "2026-08-15"}
                  ]
                }
                """);

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Sofia", "Karim", "Lina")));

        createExpense(owner, tripId, """
                {
                  "title": "Courses Carrefour",
                  "date": "2026-08-02",
                  "payerPersonId": "%s",
                  "totalAmount": 120,
                  "meatAmount": 30,
                  "alcoholAmount": 20,
                  "customConstraintAmounts": {"Sans porc": 10},
                  "type": "NORMAL",
                  "advancedMode": false,
                  "manualParticipantIds": [],
                  "currency": "EUR",
                  "exchangeRateToTripCurrency": 1
                }
                """.formatted(sofiaId));

        createExpense(owner, tripId, """
                {
                  "title": "Essence mutualisée",
                  "date": "2026-08-03",
                  "payerPersonId": "%s",
                  "totalAmount": 250,
                  "meatAmount": 0,
                  "alcoholAmount": 0,
                  "type": "GLOBAL",
                  "advancedMode": false,
                  "manualParticipantIds": [],
                  "currency": "EUR",
                  "exchangeRateToTripCurrency": 1
                }
                """.formatted(karimId));

        mockMvc.perform(get("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Courses Carrefour"))
                .andExpect(jsonPath("$[0].customConstraintAmounts['Sans porc']").value(10.0));

        mockMvc.perform(get("/api/v1/trips/{tripId}/summary", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceCurrency").value("EUR"))
                .andExpect(jsonPath("$.balances", hasSize(3)))
                .andExpect(jsonPath("$.settlements", hasSize(2)))
                .andExpect(jsonPath("$.balances[*].personName", containsInAnyOrder("Sofia", "Karim", "Lina")));

        mockMvc.perform(get("/api/v1/trips/{tripId}/exports/expenses.csv", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Courses Carrefour")));

        mockMvc.perform(get("/api/v1/trips/{tripId}/exports/summary.csv", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sofia")));

        mockMvc.perform(get("/api/v1/trips/{tripId}/audit-logs", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))));
    }

    @Test
    void globalExpensePersistsCustomConstraintAmountsAndUsesThemInSummary() throws Exception {
        AuthSession owner = registerUser("global-custom-owner@example.com");
        UUID tripId = createTrip(owner);

        UUID sofiaId = createPerson(owner, tripId, simplePersonJson("Sofia", "1000"));
        createPerson(owner, tripId, """
                {
                  "name": "Karim",
                  "livingRest": 1000,
                  "weightMode": "LIVING_REST",
                  "advancedLivingRest": false,
                  "vegetarian": false,
                  "noAlcohol": false,
                  "customConstraints": ["Sans porc"],
                  "presencePeriods": [
                    {"startDate": "2026-08-10", "endDate": "2026-08-15"}
                  ]
                }
                """);
        createPerson(owner, tripId, simplePersonJson("Lina", "1000"));

        createExpense(owner, tripId, """
                {
                  "title": "Courses mutualisées avec contrainte",
                  "date": "2026-08-02",
                  "payerPersonId": "%s",
                  "totalAmount": 120,
                  "meatAmount": 0,
                  "alcoholAmount": 0,
                  "customConstraintAmounts": {"Sans porc": 30},
                  "type": "GLOBAL",
                  "advancedMode": false,
                  "manualParticipantIds": [],
                  "currency": "EUR",
                  "exchangeRateToTripCurrency": 1
                }
                """.formatted(sofiaId));

        mockMvc.perform(get("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("GLOBAL"))
                .andExpect(jsonPath("$[0].customConstraintAmounts['Sans porc']").value(30.0));

        mockMvc.perform(get("/api/v1/trips/{tripId}/summary", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[?(@.personName == 'Sofia')].totalOwed", contains(45.0)))
                .andExpect(jsonPath("$.balances[?(@.personName == 'Karim')].totalOwed", contains(30.0)))
                .andExpect(jsonPath("$.balances[?(@.personName == 'Lina')].totalOwed", contains(45.0)))
                .andExpect(jsonPath("$.balances[?(@.personName == 'Sofia')].balance", contains(75.0)));
    }

    @Test
    void authenticationAndTripMembershipAreRequired() throws Exception {
        AuthSession owner = registerUser("security-owner@example.com");
        AuthSession outsider = registerUser("security-outsider@example.com");
        UUID tripId = createTrip(owner);

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trips/{tripId}/exports/expenses.csv", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trips/{tripId}/exports/summary.csv", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trips/{tripId}/audit-logs", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trips/{tripId}/invitations", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        AuthSession owner = registerUser("expired-token-owner@example.com");

        userSessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(owner.userId()).forEach(session -> {
            session.setExpiresAt(Instant.now().minusSeconds(60));
            userSessionRepository.save(session);
        });

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.details[0]").value("Authentification requise."));
    }

    @Test
    void logoutInvalidatesAccessToken() throws Exception {
        AuthSession owner = registerUser("logout-owner@example.com");
        UUID tripId = createTrip(owner);

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invitationJoinGuestLinkingAndPrivateRavAreControlled() throws Exception {
        AuthSession owner = registerUser("privacy-owner@example.com");
        UUID tripId = createTrip(owner);
        UUID guestId = createPerson(owner, tripId, simplePersonJson("Nvoskerjen guest", "1000"));

        MvcResult invitationResult = mockMvc.perform(post("/api/v1/trips/{tripId}/invitations", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roleToGrant": "PARTICIPANT", "expiresInDays": 7 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();
        String invitationCode = read(invitationResult).get("code").asText();

        AuthSession member = registerUser("privacy-member@example.com");
        mockMvc.perform(put("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Nvoskerjen",
                                  "livingRest": 1500,
                                  "weightMode": "LIVING_REST",
                                  "advancedLivingRest": false,
                                  "vegetarian": false,
                                  "noAlcohol": true,
                                  "livingRestPublic": false,
                                  "customConstraints": ["Sans porc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.livingRestPublic").value(false));

        mockMvc.perform(post("/api/v1/trips/{tripId}/join", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestPersonId": "%s",
                                  "applyProfileToGuest": false,
                                  "invitationCode": "%s"
                                }
                                """.formatted(guestId, invitationCode)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].livingRestHidden", contains(true)))
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].livingRest", contains(nullValue())));

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].livingRestHidden", contains(false)))
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].livingRest", contains(1000.0)))
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].noAlcohol", contains(false)));

        mockMvc.perform(put("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Nvoskerjen",
                                  "livingRest": 1900,
                                  "weightMode": "LIVING_REST",
                                  "advancedLivingRest": false,
                                  "vegetarian": false,
                                  "noAlcohol": true,
                                  "livingRestPublic": true,
                                  "customConstraints": ["Sans porc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedPersons[0].personId").value(guestId.toString()));

        mockMvc.perform(post("/api/v1/auth/profile/apply-to-linked-persons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "personIds": ["%s"] }
                                """.formatted(guestId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].livingRest", contains(1900.0)))
                .andExpect(jsonPath("$[?(@.name == 'Nvoskerjen guest')].noAlcohol", contains(true)));

        mockMvc.perform(get("/api/v1/trips/{tripId}/audit-logs", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(member)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trips/{tripId}/audit-logs", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("privacy-member@example.com"))));
    }

    @Test
    void participantCannotCreateGuestOrManageTripSettings() throws Exception {
        AuthSession owner = registerUser("roles-owner@example.com");
        UUID tripId = createTrip(owner);
        String invitationCode = createInvitationCode(owner, tripId);
        AuthSession participant = registerUser("roles-participant@example.com");

        mockMvc.perform(post("/api/v1/trips/{tripId}/join", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "invitationCode": "%s" }
                                """.formatted(invitationCode)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simplePersonJson("Guest refusé", "1000")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons/current-user", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Participant lié",
                                  "applyProfileToPerson": false,
                                  "presencePeriods": [
                                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Participant lié"))
                .andExpect(jsonPath("$.guest").value(false))
                .andExpect(jsonPath("$.linkedUserId").value(participant.userId().toString()))
                .andExpect(jsonPath("$.weightMode").value("AVERAGE"));

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons/current-user", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Participant doublon",
                                  "applyProfileToPerson": false,
                                  "presencePeriods": [
                                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("déjà lié")));

        mockMvc.perform(put("/api/v1/trips/{tripId}/constraints", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customConstraints": ["Sans lactose"] }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordChangeRotatesTokenAndOldTokenIsRejected() throws Exception {
        AuthSession owner = registerUser("password-owner@example.com");

        MvcResult result = mockMvc.perform(put("/api/v1/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "motdepassefort",
                                  "newPassword": "nouveauMotdepasseFort"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.note", containsString("Mot de passe modifié")))
                .andReturn();
        AuthSession rotated = new AuthSession(owner.userId(), read(result).get("accessToken").asText());

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(rotated)))
                .andExpect(status().isOk());
    }

    @Test
    void emailVerificationTokenConfirmsEmailAndReturnsSession() throws Exception {
        AuthSession owner = registerUser("verify-owner@example.com");
        String rawToken = "valid-email-verification-token";
        createEmailVerificationToken(owner.userId(), "verify-owner@example.com", rawToken, Instant.now().plusSeconds(3600), null);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "token": "%s" }
                                """.formatted(rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        AuthSession verified = new AuthSession(owner.userId(), read(result).get("accessToken").asText());
        mockMvc.perform(get("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(verified)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void passwordResetRejectsExpiredAndUsedTokensAndInvalidatesExistingSessions() throws Exception {
        AuthSession owner = registerUser("reset-owner@example.com");
        AuthSession secondSession = loginUser("reset-owner@example.com");

        String expiredToken = "expired-reset-token";
        createPasswordResetToken(owner.userId(), expiredToken, Instant.now().minusSeconds(3600), null);
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "nouveauMotdepasseFort"
                                }
                                """.formatted(expiredToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("invalide ou expiré")));

        String usedToken = "used-reset-token";
        createPasswordResetToken(owner.userId(), usedToken, Instant.now().plusSeconds(3600), Instant.now());
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "nouveauMotdepasseFort"
                                }
                                """.formatted(usedToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("invalide ou expiré")));

        String validToken = "valid-reset-token";
        createPasswordResetToken(owner.userId(), validToken, Instant.now().plusSeconds(3600), null);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "nouveauMotdepasseFort"
                                }
                                """.formatted(validToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        AuthSession resetSession = new AuthSession(owner.userId(), read(result).get("accessToken").asText());

        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(secondSession)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(resetSession)))
                .andExpect(status().isOk());
    }

    @Test
    void multiSessionListSpecificRevokeAndGlobalRevokeWork() throws Exception {
        AuthSession owner = registerUser("session-owner@example.com");
        AuthSession secondSession = loginUser("session-owner@example.com");

        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andReturn();
        JsonNode sessions = read(sessionsResult);
        String secondSessionId = null;
        for (JsonNode session : sessions) {
            if (!session.get("current").asBoolean()) {
                secondSessionId = session.get("sessionId").asText();
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(secondSessionId);

        mockMvc.perform(delete("/api/v1/auth/sessions/{sessionId}", secondSessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(secondSession)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/profile").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountExportAndDeletionAnonymizePersonalData() throws Exception {
        AuthSession owner = registerUser("rgpd-owner@example.com");
        UUID tripId = createTrip(owner);
        createPerson(owner, tripId, simplePersonJson("RGPD Owner", "1500"));

        mockMvc.perform(get("/api/v1/auth/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("rgpd-owner@example.com"))
                .andExpect(jsonPath("$.trips", hasSize(1)))
                .andExpect(jsonPath("$.exportedAt").exists());

        mockMvc.perform(delete("/api/v1/auth/account")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isUnauthorized());

        userRepository.findById(owner.userId()).ifPresent(user -> {
            org.junit.jupiter.api.Assertions.assertTrue(user.getEmail().startsWith("deleted-"));
            org.junit.jupiter.api.Assertions.assertNull(user.getSessionTokenHash());
        });
    }

    @Test
    void validationRulesRejectBadTripPresenceExpenseAndConstraintData() throws Exception {
        AuthSession owner = registerUser("validation@example.com");

        mockMvc.perform(post("/api/v1/trips")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Voyage impossible",
                                  "startDate": "2026-08-15",
                                  "endDate": "2026-08-01",
                                  "referenceCurrency": "EUR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("date de début")));

        UUID tripId = createTrip(owner);
        UUID sofiaId = createPerson(owner, tripId, """
                {
                  "name": "Sofia",
                  "livingRest": 1000,
                  "weightMode": "LIVING_REST",
                  "advancedLivingRest": false,
                  "vegetarian": false,
                  "noAlcohol": false,
                  "presencePeriods": [
                    {"startDate": "2026-08-01", "endDate": "2026-08-05"}
                  ]
                }
                """);

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Présence chevauchée",
                                  "livingRest": 1000,
                                  "weightMode": "LIVING_REST",
                                  "advancedLivingRest": false,
                                  "vegetarian": false,
                                  "noAlcohol": false,
                                  "presencePeriods": [
                                    {"startDate": "2026-08-01", "endDate": "2026-08-05"},
                                    {"startDate": "2026-08-05", "endDate": "2026-08-10"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("ne doivent pas se chevaucher")));

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simplePersonJson("  sofia  ", "1200")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("porte déjà ce nom")));

        mockMvc.perform(post("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Contrainte inconnue",
                                  "livingRest": 1000,
                                  "weightMode": "LIVING_REST",
                                  "advancedLivingRest": false,
                                  "vegetarian": false,
                                  "noAlcohol": false,
                                  "customConstraints": ["Sans lactose"],
                                  "presencePeriods": [
                                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("n'est pas déclarée")));

        mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dépense hors présence",
                                  "date": "2026-08-10",
                                  "payerPersonId": "%s",
                                  "totalAmount": 50,
                                  "meatAmount": 0,
                                  "alcoholAmount": 0,
                                  "type": "NORMAL",
                                  "advancedMode": false,
                                  "manualParticipantIds": [],
                                  "currency": "EUR",
                                  "exchangeRateToTripCurrency": 1
                                }
                                """.formatted(sofiaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("part générale ne concerne personne")));

        mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Détails supérieurs au total",
                                  "date": "2026-08-02",
                                  "payerPersonId": "%s",
                                  "totalAmount": 50,
                                  "meatAmount": 30,
                                  "alcoholAmount": 30,
                                  "type": "NORMAL",
                                  "advancedMode": false,
                                  "manualParticipantIds": [],
                                  "currency": "EUR",
                                  "exchangeRateToTripCurrency": 1
                                }
                                """.formatted(sofiaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("dépasser le total")));

        mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Dépense négative",
                                  "date": "2026-08-02",
                                  "payerPersonId": "%s",
                                  "totalAmount": -1,
                                  "meatAmount": 0,
                                  "alcoholAmount": 0,
                                  "type": "NORMAL",
                                  "advancedMode": false,
                                  "manualParticipantIds": [],
                                  "currency": "EUR",
                                  "exchangeRateToTripCurrency": 1
                                }
                                """.formatted(sofiaId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("totalAmount")));
    }

    private AuthSession registerUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Joey",
                                  "password": "motdepassefort"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        JsonNode auth = read(result);
        return new AuthSession(UUID.fromString(auth.get("userId").asText()), auth.get("accessToken").asText());
    }

    private UUID createTrip(AuthSession owner) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trips")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Vacances août 2026",
                                  "startDate": "2026-08-01",
                                  "endDate": "2026-08-15",
                                  "referenceCurrency": "EUR",
                                  "customConstraints": ["Sans porc"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        return UUID.fromString(read(result).get("id").asText());
    }

    private UUID createPerson(AuthSession actor, UUID tripId, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        return UUID.fromString(read(result).get("id").asText());
    }

    private UUID createExpense(AuthSession actor, UUID tripId, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trips/{tripId}/expenses", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        return UUID.fromString(read(result).get("id").asText());
    }

    private String createInvitationCode(AuthSession actor, UUID tripId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trips/{tripId}/invitations", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "roleToGrant": "PARTICIPANT", "expiresInDays": 7 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andReturn();
        return read(result).get("code").asText();
    }

    private AuthSession loginUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "motdepassefort"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
        JsonNode auth = read(result);
        return new AuthSession(UUID.fromString(auth.get("userId").asText()), auth.get("accessToken").asText());
    }

    private void createEmailVerificationToken(UUID userId, String email, String rawToken, Instant expiresAt, Instant usedAt) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setUser(user);
        token.setEmail(email);
        token.setTokenHash(sessionTokenService.hashToken(rawToken));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        emailVerificationTokenRepository.save(token);
    }

    private void createPasswordResetToken(UUID userId, String rawToken, Instant expiresAt, Instant usedAt) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setUser(user);
        token.setTokenHash(sessionTokenService.hashToken(rawToken));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        passwordResetTokenRepository.save(token);
    }

    private String simplePersonJson(String name, String livingRest) {
        return """
                {
                  "name": "%s",
                  "livingRest": %s,
                  "weightMode": "LIVING_REST",
                  "advancedLivingRest": false,
                  "vegetarian": false,
                  "noAlcohol": false,
                  "presencePeriods": [
                    {"startDate": "2026-08-01", "endDate": "2026-08-15"}
                  ]
                }
                """.formatted(name, livingRest);
    }

    private String bearer(AuthSession session) {
        return "Bearer " + session.token();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record AuthSession(UUID userId, String token) {}
}
