package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.PersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<PersonEntity, UUID> {
    List<PersonEntity> findByTripIdOrderByNameAsc(UUID tripId);
    Optional<PersonEntity> findByTripIdAndNormalizedName(UUID tripId, String normalizedName);
    boolean existsByTripIdAndNormalizedName(UUID tripId, String normalizedName);
    List<PersonEntity> findByLinkedUserId(UUID userId);
    Optional<PersonEntity> findByTripIdAndLinkedUserId(UUID tripId, UUID userId);
}
