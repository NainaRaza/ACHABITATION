package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.AccountExportAuditLog;
import fr.achabitation.api.dto.AuthDtos.AccountExportExpense;
import fr.achabitation.api.dto.AuthDtos.AccountExportInvitation;
import fr.achabitation.api.dto.AuthDtos.AccountExportLinkedPerson;
import fr.achabitation.api.dto.AuthDtos.AccountExportResponse;
import fr.achabitation.api.dto.AuthDtos.AccountExportTrip;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.AuditLogRepository;
import fr.achabitation.infrastructure.repository.ExpenseRepository;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.TripInvitationRepository;
import fr.achabitation.infrastructure.repository.TripMemberRepository;
import fr.achabitation.infrastructure.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountDataService {
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final EntityMapper mapper;
    private final TripMemberRepository tripMemberRepository;
    private final TripInvitationRepository tripInvitationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ExpenseRepository expenseRepository;
    private final UserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;
    private final AccountSessionService accountSessionService;
    private final SecurityEventService securityEventService;

    public AccountDataService(
            UserRepository userRepository,
            PersonRepository personRepository,
            EntityMapper mapper,
            TripMemberRepository tripMemberRepository,
            TripInvitationRepository tripInvitationRepository,
            AuditLogRepository auditLogRepository,
            ExpenseRepository expenseRepository,
            UserProfileService userProfileService,
            PasswordEncoder passwordEncoder,
            SessionTokenService sessionTokenService,
            AccountSessionService accountSessionService,
            SecurityEventService securityEventService
    ) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.mapper = mapper;
        this.tripMemberRepository = tripMemberRepository;
        this.tripInvitationRepository = tripInvitationRepository;
        this.auditLogRepository = auditLogRepository;
        this.expenseRepository = expenseRepository;
        this.userProfileService = userProfileService;
        this.passwordEncoder = passwordEncoder;
        this.sessionTokenService = sessionTokenService;
        this.accountSessionService = accountSessionService;
        this.securityEventService = securityEventService;
    }

    public AccountExportResponse exportAccount(UserEntity user) {
        UserProfileResponse profile = userProfileService.profile(user);
        List<AccountExportTrip> trips = tripMemberRepository.findByUserId(user.getId()).stream()
                .map(member -> new AccountExportTrip(
                        member.getTrip().getId(),
                        member.getTrip().getName(),
                        member.getRole() == null ? null : member.getRole().name(),
                        member.getJoinedAt()
                ))
                .toList();
        List<AccountExportLinkedPerson> linkedPersons = personRepository.findByLinkedUserId(user.getId()).stream()
                .map(person -> new AccountExportLinkedPerson(
                        person.getId(),
                        person.getName(),
                        person.getTrip().getId(),
                        person.getTrip().getName(),
                        person.isLivingRestPublic()
                ))
                .toList();
        List<AccountExportInvitation> invitations = tripInvitationRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(invitation -> new AccountExportInvitation(
                        invitation.getId(),
                        invitation.getTrip().getId(),
                        invitation.getTrip().getName(),
                        invitation.getRoleToGrant() == null ? null : invitation.getRoleToGrant().name(),
                        invitation.getCreatedAt(),
                        invitation.getExpiresAt(),
                        invitation.getRevokedAt() != null
                ))
                .toList();
        List<AccountExportAuditLog> auditLogs = auditLogRepository.findByActorIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(log -> new AccountExportAuditLog(
                        log.getId(),
                        log.getTrip().getId(),
                        log.getTrip().getName(),
                        log.getAction() == null ? null : log.getAction().name(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getCreatedAt()
                ))
                .toList();
        List<AccountExportExpense> paidExpenses = expenseRepository.findByPayerLinkedUserId(user.getId()).stream()
                .map(expense -> new AccountExportExpense(
                        expense.getId(),
                        expense.getTrip().getId(),
                        expense.getTrip().getName(),
                        expense.getTitle(),
                        mapper.money(expense.getTotalAmount()),
                        expense.getCurrency()
                ))
                .toList();
        return new AccountExportResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                profile,
                trips,
                linkedPersons,
                invitations,
                auditLogs,
                paidExpenses,
                Instant.now()
        );
    }

    public void deleteAccount(UserEntity user) {
        UUID userId = user.getId();
        for (PersonEntity person : personRepository.findByLinkedUserId(userId)) {
            anonymizeLinkedPerson(person);
        }

        securityEventService.log(user, "account.deletion_requested");
        anonymizeUser(user, userId);
        userRepository.save(user);
        accountSessionService.logoutAll(user);
    }

    private void anonymizeLinkedPerson(PersonEntity person) {
        person.setLinkedUser(null);
        String anonymizedName = "Utilisateur supprimé " + person.getId().toString().substring(0, 8);
        person.setName(anonymizedName);
        person.setNormalizedName(mapper.normalizeName(anonymizedName));
        person.setLivingRest(BigDecimal.ZERO);
        person.setNetIncomeAfterTax(BigDecimal.ZERO);
        person.setRent(BigDecimal.ZERO);
        person.setCredits(BigDecimal.ZERO);
        person.setFixedCharges(BigDecimal.ZERO);
        person.setTransport(BigDecimal.ZERO);
        person.setInsurance(BigDecimal.ZERO);
        person.setOtherMandatoryExpenses(BigDecimal.ZERO);
        person.setMenstrualProtection(BigDecimal.ZERO);
        person.setLivingRestPublic(false);
        person.getCustomConstraints().clear();
        personRepository.save(person);
    }

    private void anonymizeUser(UserEntity user, UUID userId) {
        user.setEmail("deleted-" + userId + "@deleted.achabitation.local");
        user.setDisplayName("Utilisateur supprimé");
        user.setPasswordHash(passwordEncoder.encode(sessionTokenService.newRawToken()));
        user.setSessionTokenHash(null);
        user.setSessionTokenIssuedAt(null);
        user.setLivingRest(BigDecimal.ZERO);
        user.setNetIncomeAfterTax(BigDecimal.ZERO);
        user.setRent(BigDecimal.ZERO);
        user.setCredits(BigDecimal.ZERO);
        user.setFixedCharges(BigDecimal.ZERO);
        user.setTransport(BigDecimal.ZERO);
        user.setInsurance(BigDecimal.ZERO);
        user.setOtherMandatoryExpenses(BigDecimal.ZERO);
        user.setMenstrualProtection(BigDecimal.ZERO);
        user.setVegetarian(false);
        user.setNoAlcohol(false);
        user.setLivingRestPublic(false);
        user.setEmailVerifiedAt(null);
        user.setEmailVerificationRequestedAt(null);
        user.setDeletedAt(Instant.now());
        user.getKnownCustomConstraints().clear();
        user.getCustomConstraints().clear();
    }
}
