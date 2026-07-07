package fr.achabitation.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AccountEmailService {
    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailFrom;
    private final String publicUrl;
    private final boolean enabled;

    public AccountEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${achabitation.mail.from:no-reply@achabitation.local}") String mailFrom,
            @Value("${achabitation.app.public-url:http://localhost:3000}") String publicUrl,
            @Value("${achabitation.mail.enabled:false}") boolean enabled
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailFrom = mailFrom;
        this.publicUrl = publicUrl == null || publicUrl.isBlank() ? "http://localhost:3000" : publicUrl.replaceAll("/$", "");
        this.enabled = enabled;
    }

    public void sendPasswordReset(String email, String rawToken) {
        String link = publicUrl + "/?resetToken=" + encode(rawToken);
        send(email, "Réinitialisation de ton mot de passe ACHABITATION", """
                Une demande de réinitialisation de mot de passe a été faite pour ton compte ACHABITATION.

                Ouvre ce lien pour choisir un nouveau mot de passe :
                %s

                Ce lien expire dans 30 minutes. Ignore cet email si tu n'es pas à l'origine de la demande.
                """.formatted(link));
    }

    public void sendEmailVerification(String email, String rawToken) {
        String link = publicUrl + "/?verifyEmailToken=" + encode(rawToken);
        send(email, "Vérification de ton email ACHABITATION", """
                Confirme ton adresse email pour ton compte ACHABITATION.

                Ouvre ce lien pour vérifier ton compte :
                %s

                Ce lien expire dans 24 heures. Ignore cet email si tu n'es pas à l'origine de l'inscription.
                """.formatted(link));
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("security_email_prepared target={} subject={} mode=disabled", maskEmail(to), subject);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("security_email_send_failed target={} subject={} reason=no_mail_sender", maskEmail(to), subject);
            throw new IllegalStateException("Email technique non envoyé. Vérifie la configuration SMTP.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException error) {
            log.error("security_email_send_failed target={} subject={}", maskEmail(to), subject);
            throw new IllegalStateException("Email technique non envoyé. Vérifie la configuration SMTP.");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "masked";
        String[] parts = email.split("@", 2);
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }
}
