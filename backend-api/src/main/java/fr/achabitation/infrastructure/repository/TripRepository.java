package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.TripEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<TripEntity, UUID> {
    List<TripEntity> findByActiveTrueOrderByCreatedAtDesc();
}
