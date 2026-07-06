package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.TripMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripMemberRepository extends JpaRepository<TripMemberEntity, UUID> {
    List<TripMemberEntity> findByUserId(UUID userId);
    List<TripMemberEntity> findByTripId(UUID tripId);
    Optional<TripMemberEntity> findByTripIdAndUserId(UUID tripId, UUID userId);
    boolean existsByTripIdAndUserId(UUID tripId, UUID userId);
}
