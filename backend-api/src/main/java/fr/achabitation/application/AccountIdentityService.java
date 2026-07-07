package fr.achabitation.application;

import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountIdentityService {
    private final UserRepository userRepository;

    public AccountIdentityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String normalizedEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public String normalizedDisplayName(String displayName) {
        return displayName == null ? "" : displayName.trim().replaceAll("\\s+", " ");
    }

    public void ensureValidDisplayName(String displayName) {
        String normalized = normalizedDisplayName(displayName);
        if (normalized.length() < 2 || normalized.length() > 120) {
            throw new IllegalArgumentException("Le nom affiché doit contenir entre 2 et 120 caractères.");
        }
    }

    @Transactional(readOnly = true)
    public UserEntity findUserByEmailOrDisplayName(String identifier) {
        String raw = identifier == null ? "" : identifier.trim();
        if (raw.isBlank()) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        if (raw.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalizedEmail(raw))
                    .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides."));
        }
        List<UserEntity> matches = userRepository.findAllByDisplayNameIgnoreCase(normalizedDisplayName(raw));
        if (matches == null || matches.size() != 1) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        return matches.get(0);
    }
}
