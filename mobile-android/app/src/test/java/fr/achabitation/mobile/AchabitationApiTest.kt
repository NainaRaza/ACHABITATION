package fr.achabitation.mobile

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

class AchabitationApiTest {
    private var server: HttpServer? = null

    @After
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun logout_calls_backend_with_current_access_token() = runTest {
        val method = AtomicReference<String>()
        val authorization = AtomicReference<String>()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).also { httpServer ->
            httpServer.createContext("/api/v1/auth/logout") { exchange ->
                method.set(exchange.requestMethod)
                authorization.set(exchange.requestHeaders.getFirst("Authorization"))
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
            }
            httpServer.start()
        }

        val api = AchabitationApi(
            baseUrlProvider = { "http://127.0.0.1:${server!!.address.port}/api/v1" },
            tokenProvider = { "token-123" }
        )

        api.logout()

        assertEquals("POST", method.get())
        assertEquals("Bearer token-123", authorization.get())
    }

    @Test
    fun logout_propagates_unauthorized_status() = runTest {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).also { httpServer ->
            httpServer.createContext("/api/v1/auth/logout") { exchange ->
                val payload = """{"error":"Unauthorized","details":["Authentification requise."]}"""
                exchange.responseHeaders.add("Content-Type", "application/json;charset=UTF-8")
                exchange.sendResponseHeaders(401, payload.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(payload.toByteArray()) }
                exchange.close()
            }
            httpServer.start()
        }

        val api = AchabitationApi(
            baseUrlProvider = { "http://127.0.0.1:${server!!.address.port}/api/v1" },
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
