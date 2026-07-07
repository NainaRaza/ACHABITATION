package fr.achabitation.mobile

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AchabitationApiTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun logout_calls_backend_with_current_access_token() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val api = AchabitationApi(
            baseUrlProvider = { server.url("/api/v1").toString().removeSuffix("/") },
            tokenProvider = { "token-123" }
        )

        api.logout()

        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals("POST", request.method)
        assertEquals("Bearer token-123", request.getHeader("Authorization"))
    }

    @Test
    fun logout_propagates_unauthorized_status() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json;charset=UTF-8")
                .setBody("""{"error":"Unauthorized","details":["Authentification requise."]}""")
        )

        val api = AchabitationApi(
            baseUrlProvider = { server.url("/api/v1").toString().removeSuffix("/") },
            tokenProvider = { "token-123" }
        )

        try {
            api.logout()
            fail("Une ApiException était attendue.")
        } catch (ex: ApiException) {
            assertEquals(401, ex.status)
        }
    }
}
