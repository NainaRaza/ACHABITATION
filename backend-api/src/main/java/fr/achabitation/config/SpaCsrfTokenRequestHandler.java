package fr.achabitation.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Gestion CSRF adaptée aux clients SPA.
 *
 * Le front web lit le cookie XSRF-TOKEN et le renvoie tel quel dans le header
 * X-XSRF-TOKEN. Le handler CSRF par défaut de Spring Security 6 applique une
 * protection BREACH par token masqué côté rendu serveur ; ce handler conserve
 * cette protection pour les usages serveur, mais accepte le token brut envoyé
 * par une SPA via header.
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        return xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
