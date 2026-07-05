package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.TripInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripInvitationRepository extends JpaRepository<TripInvitationEntity, UUID> {
    Optional<TripInvitationEntity> findByCode(String code);
    List<TripInvitationEntity> findByTripIdOrderByCreatedAtDesc(UUID tripId);
}
