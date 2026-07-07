package fr.achabitation.infrastructure.repository;

import fr.achabitation.infrastructure.entity.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {
    List<ExpenseEntity> findByTripIdOrderByDateAscTitleAsc(UUID tripId);
    List<ExpenseEntity> findByPayerLinkedUserId(UUID linkedUserId);
}
