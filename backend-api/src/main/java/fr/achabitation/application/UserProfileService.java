package fr.achabitation.application;

import fr.achabitation.api.dto.AuthDtos.ApplyProfileToLinkedPersonsRequest;
import fr.achabitation.api.dto.AuthDtos.LinkedProfilePersonResponse;
import fr.achabitation.api.dto.AuthDtos.UserProfileRequest;
import fr.achabitation.api.dto.AuthDtos.UserProfileResponse;
import fr.achabitation.domain.model.WeightMode;
import fr.achabitation.domain.util.ConstraintNameUtils;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.PersonRepository;
import fr.achabitation.infrastructure.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserProfileService {
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final EntityMapper mapper;
    private final AccountIdentityService accountIdentityService;

    public UserProfileService(UserRepository userRepository, PersonRepository personRepository, EntityMapper mapper, AccountIdentityService accountIdentityService) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.mapper = mapper;
        this.accountIdentityService = accountIdentityService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(UserEntity user) {
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UserEntity user, UserProfileRequest request) {
        if (request.displayName() != null && !request.displayName().isBlank()) {
            String displayName = accountIdentityService.normalizedDisplayName(request.displayName());
            accountIdentityService.ensureValidDisplayName(displayName);
            user.setDisplayName(displayName);
        }
        applyFinancialAndConstraintProfile(user, request);
        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse applyProfileToLinkedPersons(UserEntity user, ApplyProfileToLinkedPersonsRequest request) {
        ensureValidUserProfile(user);
        Set<UUID> requestedPersonIds = request == null || request.personIds() == null ? Set.of() : request.personIds();
        if (requestedPersonIds.isEmpty()) {
            throw new IllegalArgumentException("Sélectionne au moins un voyage lié à mettre à jour.");
        }

        List<PersonEntity> linkedPersons = personRepository.findByLinkedUserId(user.getId());
        Set<UUID> allowedPersonIds = linkedPersons.stream()
                .map(PersonEntity::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (UUID personId : requestedPersonIds) {
            if (!allowedPersonIds.contains(personId)) {
                throw new IllegalArgumentException("Une personne sélectionnée n'est pas liée à ton compte.");
            }
        }

        for (PersonEntity person : linkedPersons) {
            if (requestedPersonIds.contains(person.getId())) {
                applyUserProfileToPerson(user, person);
                personRepository.save(person);
            }
        }

        return toProfileResponse(user);
    }

    public void applyFinancialAndConstraintProfile(UserEntity user, UserProfileRequest request) {
        user.setWeightMode(request.weightMode() == null ? WeightMode.LIVING_REST : request.weightMode());
        user.setAdvancedLivingRest(request.advancedLivingRest());
        user.setNetIncomeAfterTax(mapper.money(request.netIncomeAfterTax()));
        user.setRent(mapper.money(request.rent()));
        user.setCredits(mapper.money(request.credits()));
        user.setFixedCharges(mapper.money(request.fixedCharges()));
        user.setTransport(mapper.money(request.transport()));
        user.setInsurance(mapper.money(request.insurance()));
        user.setOtherMandatoryExpenses(mapper.money(request.otherMandatoryExpenses()));
        user.setMenstrualProtection(mapper.money(request.menstrualProtection()));
        user.setLivingRest(computeLivingRest(user.isAdvancedLivingRest(), request.livingRest(), user));
        user.setVegetarian(request.vegetarian());
        user.setNoAlcohol(request.noAlcohol());
        user.setLivingRestPublic(request.livingRestPublic());
        replaceUserCustomConstraints(user, request.customConstraints());
        validateUserProfileForStorage(user);
    }

    public void applyUserProfileToPerson(UserEntity user, PersonEntity person) {
        person.setLinkedUser(user);
        person.setWeightMode(user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode());
        person.setAdvancedLivingRest(user.isAdvancedLivingRest());
        person.setNetIncomeAfterTax(mapper.money(user.getNetIncomeAfterTax()));
        person.setRent(mapper.money(user.getRent()));
        person.setCredits(mapper.money(user.getCredits()));
        person.setFixedCharges(mapper.money(user.getFixedCharges()));
        person.setTransport(mapper.money(user.getTransport()));
        person.setInsurance(mapper.money(user.getInsurance()));
        person.setOtherMandatoryExpenses(mapper.money(user.getOtherMandatoryExpenses()));
        person.setMenstrualProtection(mapper.money(user.getMenstrualProtection()));
        person.setLivingRest(mapper.money(user.getLivingRest()));
        person.setLivingRestPublic(user.isLivingRestPublic());
        person.setVegetarian(user.isVegetarian());
        person.setNoAlcohol(user.isNoAlcohol());
        person.getCustomConstraints().clear();
        person.getCustomConstraints().addAll(profileConstraintsAllowedInTrip(user, person));
    }

    public Set<String> normalizeConstraints(Set<String> constraints) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (constraints == null) {
            return normalized;
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        for (String rawConstraint : constraints) {
            String displayName = ConstraintNameUtils.canonicalDisplayName(rawConstraint);
            String key = ConstraintNameUtils.key(displayName);
            if (key.isBlank()) {
                continue;
            }
            if (displayName.length() > 120) {
                throw new IllegalArgumentException("Le nom d'une contrainte personnalisée ne doit pas dépasser 120 caractères.");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Une contrainte personnalisée est renseignée plusieurs fois.");
            }
            normalized.add(displayName);
        }
        return normalized;
    }

    public void ensureValidUserProfile(UserEntity user) {
        WeightMode mode = user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode();
        if (mode != WeightMode.AVERAGE && mapper.money(user.getLivingRest()).signum() <= 0) {
            throw new IllegalArgumentException("Le reste à vivre du profil doit être strictement positif pour être appliqué à un voyage.");
        }
    }

    public UserProfileResponse toProfileResponse(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                mapper.money(user.getLivingRest()),
                (user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode()),
                user.isAdvancedLivingRest(),
                mapper.money(user.getNetIncomeAfterTax()),
                mapper.money(user.getRent()),
                mapper.money(user.getCredits()),
                mapper.money(user.getFixedCharges()),
                mapper.money(user.getTransport()),
                mapper.money(user.getInsurance()),
                mapper.money(user.getOtherMandatoryExpenses()),
                mapper.money(user.getMenstrualProtection()),
                user.isVegetarian(),
                user.isNoAlcohol(),
                user.isLivingRestPublic(),
                mapper.stringSet(user.getKnownCustomConstraints()),
                mapper.stringSet(user.getCustomConstraints()),
                linkedPersons(user)
        );
    }

    private List<LinkedProfilePersonResponse> linkedPersons(UserEntity user) {
        return personRepository.findByLinkedUserId(user.getId()).stream()
                .map(person -> new LinkedProfilePersonResponse(
                        person.getId(),
                        person.getName(),
                        person.getTrip().getId(),
                        person.getTrip().getName()
                ))
                .toList();
    }

    private Set<String> profileConstraintsAllowedInTrip(UserEntity user, PersonEntity person) {
        Set<String> allowedKeys = new LinkedHashSet<>();
        if (person.getTrip() != null && person.getTrip().getCustomConstraints() != null) {
            person.getTrip().getCustomConstraints().forEach(value -> allowedKeys.add(ConstraintNameUtils.key(value)));
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (user.getCustomConstraints() == null) {
            return result;
        }
        for (String value : user.getCustomConstraints()) {
            if (allowedKeys.contains(ConstraintNameUtils.key(value))) {
                result.add(ConstraintNameUtils.canonicalDisplayName(value));
            }
        }
        return result;
    }

    private void replaceUserCustomConstraints(UserEntity user, Set<String> requested) {
        Set<String> normalized = normalizeConstraints(requested);
        user.getKnownCustomConstraints().addAll(normalized);
        user.getCustomConstraints().clear();
        user.getCustomConstraints().addAll(normalized);
    }

    private BigDecimal computeLivingRest(boolean advanced, BigDecimal requestLivingRest, UserEntity user) {
        if (!advanced) {
            return mapper.money(requestLivingRest);
        }
        return mapper.money(user.getNetIncomeAfterTax())
                .subtract(mapper.money(user.getRent()))
                .subtract(mapper.money(user.getCredits()))
                .subtract(mapper.money(user.getFixedCharges()))
                .subtract(mapper.money(user.getTransport()))
                .subtract(mapper.money(user.getInsurance()))
                .subtract(mapper.money(user.getOtherMandatoryExpenses()))
                .subtract(mapper.money(user.getMenstrualProtection()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateUserProfileForStorage(UserEntity user) {
        WeightMode mode = user.getWeightMode() == null ? WeightMode.LIVING_REST : user.getWeightMode();
        if (mode != WeightMode.AVERAGE && mapper.money(user.getLivingRest()).signum() < 0) {
            throw new IllegalArgumentException("Le reste à vivre du profil ne peut pas être négatif.");
        }
    }
}
