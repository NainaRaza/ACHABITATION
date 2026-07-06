package fr.achabitation.application;

import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.TripMemberEntity;
import fr.achabitation.infrastructure.entity.TripRole;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.TripMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthorizationService {
    private static final Set<TripRole> WRITE_ROLES = Set.of(TripRole.OWNER, TripRole.ADMIN, TripRole.PARTICIPANT);
    private static final Set<TripRole> ADMIN_ROLES = Set.of(TripRole.OWNER, TripRole.ADMIN);

    private final TripMemberRepository tripMemberRepository;
    private final PersonRepository personRepository;

    public AuthorizationService(TripMemberRepository tripMemberRepository, PersonRepository personRepository) {
        this.tripMemberRepository = tripMemberRepository;
        this.personRepository = personRepository;
    }

    public UserEntity requireAuthenticated(UserEntity user) {
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentification requise.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public TripMemberEntity requireTripMember(UUID tripId, UserEntity user) {
        requireAuthenticated(user);
        return tripMemberRepository.findByTripIdAndUserId(tripId, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Tu n'es pas membre de ce voyage."));
    }

    @Transactional(readOnly = true)
    public TripRole requireReadable(UUID tripId, UserEntity user) {
        return requireTripMember(tripId, user).getRole();
    }

    @Transactional(readOnly = true)
    public TripRole requireWritable(UUID tripId, UserEntity user) {
        TripRole role = requireTripMember(tripId, user).getRole();
        if (!WRITE_ROLES.contains(role)) {
            throw new AccessDeniedException("Ton rôle ne permet pas de modifier ce voyage.");
        }
        return role;
    }

    @Transactional(readOnly = true)
    public TripRole requireAdmin(UUID tripId, UserEntity user) {
        TripRole role = requireTripMember(tripId, user).getRole();
        if (!ADMIN_ROLES.contains(role)) {
            throw new AccessDeniedException("Droits administrateur requis sur ce voyage.");
        }
        return role;
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(UUID tripId, UserEntity user) {
        if (user == null) return false;
        return tripMemberRepository.findByTripIdAndUserId(tripId, user.getId())
                .map(member -> ADMIN_ROLES.contains(member.getRole()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isLinkedPersonOwner(PersonEntity person, UserEntity user) {
        return user != null
                && person != null
                && person.getLinkedUser() != null
                && person.getLinkedUser().getId().equals(user.getId());
    }

    @Transactional(readOnly = true)
    public void requirePersonUpdateAllowed(PersonEntity person, UserEntity user) {
        UUID tripId = person.getTrip().getId();
        if (isAdmin(tripId, user) || isLinkedPersonOwner(person, user)) {
            return;
        }
        if (person.getLinkedUser() == null) {
            requireAdmin(tripId, user);
            return;
        }
        throw new AccessDeniedException("Tu ne peux pas modifier le profil financier ou les contraintes d'une autre personne liée.");
    }

    @Transactional(readOnly = true)
    public boolean canEditFinancialProfile(PersonEntity person, UserEntity user) {
        if (person.getLinkedUser() == null) {
            return isAdmin(person.getTrip().getId(), user);
        }
        return isLinkedPersonOwner(person, user);
    }

    @Transactional(readOnly = true)
    public List<UUID> readableTripIds(UserEntity user) {
        requireAuthenticated(user);
        return tripMemberRepository.findByUserId(user.getId()).stream()
                .sorted(Comparator.comparing(TripMemberEntity::getJoinedAt).reversed())
                .map(member -> member.getTrip().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonEntity requirePersonInReadableTrip(UUID tripId, UUID personId, UserEntity user) {
        requireReadable(tripId, user);
        return personRepository.findById(personId)
                .filter(person -> person.getTrip().getId().equals(tripId))
                .orElseThrow(() -> new IllegalArgumentException("Personne introuvable dans ce voyage."));
    }
}
