package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<UserEntity> findBySessionToken(String sessionToken);
    List<UserEntity> findAllByDisplayNameIgnoreCase(String displayName);
}
