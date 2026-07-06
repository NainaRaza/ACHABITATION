package fr.achabitation.infrastructure.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "expense_manual_participant", uniqueConstraints = @UniqueConstraint(columnNames = {"expense_id", "person_id"}))
public class ExpenseParticipantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_id")
    private ExpenseEntity expense;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id")
    private PersonEntity person;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ExpenseEntity getExpense() { return expense; }
    public void setExpense(ExpenseEntity expense) { this.expense = expense; }
    public PersonEntity getPerson() { return person; }
    public void setPerson(PersonEntity person) { this.person = person; }
}
