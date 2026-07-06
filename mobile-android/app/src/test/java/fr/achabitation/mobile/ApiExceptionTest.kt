package fr.achabitation.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiExceptionTest {
    @Test
    fun api_exception_keeps_unauthorized_status_for_session_expiration() {
        val exception = ApiException("Session expirée", 401)
        assertEquals(401, exception.status)
    }
}
