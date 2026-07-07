package fr.achabitation.application;

import fr.achabitation.api.dto.TripDtos.JoinTripByCodeRequest;
import fr.achabitation.api.dto.TripDtos.JoinTripRequest;
import fr.achabitation.api.dto.TripDtos.TripInvitationCreateRequest;
import fr.achabitation.api.dto.TripDtos.TripInvitationResponse;
import fr.achabitation.api.dto.TripDtos.TripConstraintUpdateRequest;
import fr.achabitation.api.dto.TripDtos.TripCreateRequest;
import fr.achabitation.api.dto.TripDtos.TripResponse;
import fr.achabitation.domain.util.ConstraintNameUtils;
import fr.achabitation.infrastructure.entity.AuditAction;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.TripInvitationEntity;
import fr.achabitation.infrastructure.entity.TripMemberEntity;
import fr.achabitation.infrastructure.entity.TripRole;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.TripInvitationRepository;
import fr.achabitation.infrastructure.repository.TripMemberRepository;
import fr.achabitation.infrastructure.repository.TripRepository;
import fr.achabitation.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripInvitationRepository tripInvitationRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final EntityMapper mapper;
    private final AuditService auditService;
    private final AuthService authService;
    private final AuthorizationService authorizationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public TripService(TripRepository tripRepository, TripMemberRepository tripMemberRepository, TripInvitationRepository tripInvitationRepository, UserRepository userRepository, PersonRepository personRepository, EntityMapper mapper, AuditService auditService, AuthService authService, AuthorizationService authorizationService) {
        this.tripRepository = tripRepository;
        this.tripMemberRepository = tripMemberRepository;
        this.tripInvitationRepository = tripInvitationRepository;
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.authService = authService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public TripResponse create(TripCreateRequest request, UserEntity owner) {
        authorizationService.requireAuthenticated(owner);
        validateTripDates(request);

        TripEntity trip = new TripEntity();
        trip.setName(request.name().trim());
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setReferenceCurrency(request.referenceCurrency() == null || request.referenceCurrency().isBlank() ? "EUR" : request.referenceCurrency().trim().toUpperCase());
        replaceTripConstraints(trip, request.customConstraints());
        trip = tripRepository.save(trip);

        addMemberIfMissing(trip, owner, TripRole.OWNER);
        owner.getKnownCustomConstraints().addAll(trip.getCustomConstraints());
        userRepository.save(owner);

        auditService.log(trip, owner, AuditAction.TRIP_CREATED, "TRIP", trip.getId(), "Voyage créé : " + trip.getName());
        return mapper.toTripResponse(trip);
    }

    @Transactional
    public TripResponse updateConstraints(UUID tripId, TripConstraintUpdateRequest request, UserEntity actor) {
        authorizationService.requireAdmin(tripId, actor);
        TripEntity trip = getRequired(tripId);
        replaceTripConstraints(trip, request.customConstraints());
        trip = tripRepository.save(trip);
        if (actor != null) {
            actor.getKnownCustomConstraints().addAll(trip.getCustomConstraints());
            userRepository.save(actor);
        }
        auditService.log(trip, actor, AuditAction.TRIP_UPDATED, "TRIP", trip.getId(), "Contraintes personnalisées du voyage mises à jour.");
        return mapper.toTripResponse(trip);
    }


    @Transactional
    public TripResponse joinByCode(JoinTripByCodeRequest request, UserEntity user) {
        authorizationService.requireAuthenticated(user);
        TripInvitationEntity invitation = validateInvitationByCode(request == null ? null : request.invitationCode());
        TripEntity trip = invitation.getTrip();
        TripRole roleToGrant = invitation.getRoleToGrant() == null ? TripRole.PARTICIPANT : invitation.getRoleToGrant();

        addMemberIfMissing(trip, user, roleToGrant);
        user.getKnownCustomConstraints().addAll(trip.getCustomConstraints());
        userRepository.save(user);

        if (request != null && request.guestPersonId() != null) {
            linkGuestToUser(trip.getId(), request.guestPersonId(), user, request.applyProfileToGuest());
        }

        auditService.log(trip, user, AuditAction.TRIP_MEMBER_JOINED, "TRIP", trip.getId(), user.getDisplayName() + " a rejoint le voyage avec un code d'invitation.");
        return mapper.toTripResponse(trip);
    }

    @Transactional
    public TripResponse join(UUID tripId, JoinTripRequest request, UserEntity user) {
        authorizationService.requireAuthenticated(user);
        TripEntity trip = getRequired(tripId);
        boolean alreadyMember = tripMemberRepository.existsByTripIdAndUserId(tripId, user.getId());
        TripRole roleToGrant = TripRole.PARTICIPANT;
        if (!alreadyMember) {
            TripInvitationEntity invitation = validateInvitation(tripId, request == null ? null : request.invitationCode());
            roleToGrant = invitation.getRoleToGrant() == null ? TripRole.PARTICIPANT : invitation.getRoleToGrant();
        }
        addMemberIfMissing(trip, user, roleToGrant);
        user.getKnownCustomConstraints().addAll(trip.getCustomConstraints());
        userRepository.save(user);

        if (request != null && request.guestPersonId() != null) {
            linkGuestToUser(tripId, request.guestPersonId(), user, request.applyProfileToGuest());
        }

        auditService.log(trip, user, AuditAction.TRIP_MEMBER_JOINED, "TRIP", trip.getId(), user.getDisplayName() + " a rejoint le voyage.");
        return mapper.toTripResponse(trip);
    }

    @Transactional
    public PersonEntity linkGuestToUser(UUID tripId, UUID personId, UserEntity user) {
        return linkGuestToUser(tripId, personId, user, false);
    }

    @Transactional
    public PersonEntity linkGuestToUser(UUID tripId, UUID personId, UserEntity user, boolean applyProfileToGuest) {
        TripEntity trip = getRequired(tripId);
        authorizationService.requireReadable(tripId, user);
        if (applyProfileToGuest) {
            authService.ensureValidUserProfile(user);
        }
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Personne introuvable."));
        if (!person.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette personne n'appartient pas au voyage indiqué.");
        }
        if (person.getLinkedUser() != null && !person.getLinkedUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Cette personne est déjà liée à un autre compte.");
        }
        personRepository.findByTripIdAndLinkedUserId(tripId, user.getId())
                .filter(existing -> !existing.getId().equals(personId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Ce compte est déjà lié à une autre personne dans ce voyage."); });
        addMemberIfMissing(trip, user, TripRole.PARTICIPANT);
        user.getKnownCustomConstraints().addAll(trip.getCustomConstraints());
        userRepository.save(user);
        person.setLinkedUser(user);
        if (applyProfileToGuest) {
            authService.applyUserProfileToPerson(user, person);
        }
        person = personRepository.save(person);
        String detail = applyProfileToGuest
                ? "Le guest " + person.getName() + " est lié à un compte utilisateur et le profil a été appliqué."
                : "Le guest " + person.getName() + " est lié à un compte utilisateur sans écraser ses données.";
        auditService.log(trip, user, AuditAction.PERSON_LINKED_TO_USER, "PERSON", person.getId(), detail);
        return person;
    }

    @Transactional
    public TripInvitationResponse createInvitation(UUID tripId, TripInvitationCreateRequest request, UserEntity actor) {
        authorizationService.requireAdmin(tripId, actor);
        TripEntity trip = getRequired(tripId);
        TripInvitationEntity invitation = new TripInvitationEntity();
        invitation.setTrip(trip);
        invitation.setCreatedBy(actor);
        invitation.setCode(generateInvitationCode());
        TripRole requestedRole = request == null ? TripRole.PARTICIPANT : request.roleToGrant();
        if (requestedRole == null || requestedRole == TripRole.OWNER) {
            requestedRole = TripRole.PARTICIPANT;
        }
        invitation.setRoleToGrant(requestedRole);
        int days = request == null || request.expiresInDays() == null ? 7 : Math.max(1, Math.min(30, request.expiresInDays()));
        invitation.setExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));
        invitation = tripInvitationRepository.save(invitation);
        auditService.log(trip, actor, AuditAction.INVITATION_CREATED, "TRIP_INVITATION", invitation.getId(), "Invitation créée pour le voyage.");
        return toInvitationResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<TripInvitationResponse> listInvitations(UUID tripId, UserEntity actor) {
        authorizationService.requireAdmin(tripId, actor);
        return tripInvitationRepository.findByTripIdOrderByCreatedAtDesc(tripId).stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID tripId, UUID invitationId, UserEntity actor) {
        authorizationService.requireAdmin(tripId, actor);
        TripInvitationEntity invitation = tripInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable."));
        if (!invitation.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette invitation n'appartient pas au voyage indiqué.");
        }
        invitation.setRevokedAt(Instant.now());
        tripInvitationRepository.save(invitation);
        auditService.log(invitation.getTrip(), actor, AuditAction.INVITATION_REVOKED, "TRIP_INVITATION", invitation.getId(), "Invitation révoquée.");
    }


    private TripInvitationEntity validateInvitationByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code d'invitation requis pour rejoindre un voyage.");
        }
        TripInvitationEntity invitation = tripInvitationRepository.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));
        if (!invitation.isUsable(Instant.now())) {
            throw new IllegalArgumentException("Ce code d'invitation est expiré ou révoqué.");
        }
        return invitation;
    }

    private TripInvitationEntity validateInvitation(UUID tripId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code d'invitation requis pour rejoindre ce voyage.");
        }
        TripInvitationEntity invitation = tripInvitationRepository.findByCode(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Code d'invitation invalide."));
        if (!invitation.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Ce code d'invitation ne correspond pas à ce voyage.");
        }
        if (!invitation.isUsable(Instant.now())) {
            throw new IllegalArgumentException("Ce code d'invitation est expiré ou révoqué.");
        }
        return invitation;
    }

    private TripInvitationResponse toInvitationResponse(TripInvitationEntity invitation) {
        return new TripInvitationResponse(
                invitation.getId(),
                invitation.getTrip().getId(),
                invitation.getCode(),
                invitation.getRoleToGrant(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getRevokedAt() != null,
                invitation.isUsable(Instant.now())
        );
    }

    private String generateInvitationCode() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void addMemberIfMissing(TripEntity trip, UserEntity user, TripRole role) {
        if (tripMemberRepository.existsByTripIdAndUserId(trip.getId(), user.getId())) {
            return;
        }
        TripMemberEntity member = new TripMemberEntity();
        member.setTrip(trip);
        member.setUser(user);
        member.setRole(role);
        tripMemberRepository.save(member);
    }

    private void replaceTripConstraints(TripEntity trip, Set<String> constraints) {
        trip.getCustomConstraints().clear();
        trip.getCustomConstraints().addAll(authService.normalizeConstraints(constraints));
    }

    private void validateTripDates(TripCreateRequest request) {
        if (request.startDate() == null || request.endDate() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin du voyage sont obligatoires.");
        }
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("La date de début du voyage doit être antérieure ou égale à sa date de fin.");
        }
    }

    @Transactional(readOnly = true)
    public List<TripResponse> list(UserEntity user) {
        authorizationService.requireAuthenticated(user);
        return tripMemberRepository.findByUserId(user.getId()).stream()
                .map(TripMemberEntity::getTrip)
                .filter(TripEntity::isActive)
                .distinct()
                .map(mapper::toTripResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripEntity getRequired(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Voyage introuvable."));
    }
}
