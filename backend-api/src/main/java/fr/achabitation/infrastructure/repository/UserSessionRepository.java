package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {
    Optional<UserSessionEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    List<UserSessionEntity> findByUserIdAndRevokedAtIsNullOrderByLastUsedAtDesc(UUID userId);
    long countByUserIdAndRevokedAtIsNull(UUID userId);
    boolean existsByTokenHash(String tokenHash);
}
