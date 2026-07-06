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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
    void authenticationAndTripMembershipAreRequired() throws Exception {
        AuthSession owner = registerUser("security-owner@example.com");
        AuthSession outsider = registerUser("security-outsider@example.com");
        UUID tripId = createTrip(owner);

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trips/{tripId}/persons", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isForbidden());
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
