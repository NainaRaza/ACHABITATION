package fr.achabitation.application;

import fr.achabitation.api.dto.PersonDtos.PersonCreateRequest;
import fr.achabitation.api.dto.PersonDtos.PersonResponse;
import fr.achabitation.api.dto.PersonDtos.PersonUpdateRequest;
import fr.achabitation.api.dto.PersonDtos.PresencePeriodRequest;
import fr.achabitation.domain.model.WeightMode;
import fr.achabitation.domain.util.ConstraintNameUtils;
import fr.achabitation.infrastructure.entity.AuditAction;
import fr.achabitation.infrastructure.entity.PersonEntity;
import fr.achabitation.infrastructure.entity.PresencePeriodEntity;
import fr.achabitation.infrastructure.entity.TripEntity;
import fr.achabitation.infrastructure.entity.UserEntity;
import fr.achabitation.infrastructure.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final TripService tripService;
    private final EntityMapper mapper;
    private final AuditService auditService;
    private final AuthorizationService authorizationService;

    public PersonService(PersonRepository personRepository, TripService tripService, EntityMapper mapper, AuditService auditService, AuthorizationService authorizationService) {
        this.personRepository = personRepository;
        this.tripService = tripService;
        this.mapper = mapper;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public PersonResponse create(UUID tripId, PersonCreateRequest request, UserEntity viewer) {
        authorizationService.requireAdmin(tripId, viewer);
        TripEntity trip = tripService.getRequired(tripId);
        String normalizedName = mapper.normalizeName(request.name());
        if (personRepository.existsByTripIdAndNormalizedName(tripId, normalizedName)) {
            throw new IllegalArgumentException("Une personne porte déjà ce nom dans ce voyage.");
        }

        PersonEntity person = new PersonEntity();
        person.setTrip(trip);
        applyCreate(person, request, normalizedName);
        person = personRepository.save(person);
        auditService.log(trip, viewer, AuditAction.PERSON_CREATED, "PERSON", person.getId(), "Personne créée : " + person.getName());
        return mapper.toPersonResponse(person, viewer);
    }

    @Transactional
    public PersonResponse update(UUID tripId, UUID personId, PersonUpdateRequest request, UserEntity viewer) {
        authorizationService.requireReadable(tripId, viewer);
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Personne introuvable."));
        if (!person.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette personne n'appartient pas au voyage indiqué.");
        }

        String normalizedName = mapper.normalizeName(request.name());
        personRepository.findByTripIdAndNormalizedName(tripId, normalizedName)
                .filter(existing -> !existing.getId().equals(personId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Une autre personne porte déjà ce nom dans ce voyage."); });

        authorizationService.requirePersonUpdateAllowed(person, viewer);
        boolean canEditFinancialProfile = authorizationService.canEditFinancialProfile(person, viewer);
        applyUpdate(person, request, normalizedName, canEditFinancialProfile);
        person = personRepository.save(person);
        auditService.log(person.getTrip(), viewer, AuditAction.PERSON_UPDATED, "PERSON", person.getId(), "Personne modifiée : " + person.getName());
        return mapper.toPersonResponse(person, viewer);
    }

    @Transactional
    public PersonResponse linkToCurrentUser(UUID tripId, UUID personId, UserEntity user) {
        return linkToCurrentUser(tripId, personId, user, false);
    }

    @Transactional
    public PersonResponse linkToCurrentUser(UUID tripId, UUID personId, UserEntity user, boolean applyProfileToGuest) {
        PersonEntity person = tripService.linkGuestToUser(tripId, personId, user, applyProfileToGuest);
        return mapper.toPersonResponse(person, user);
    }

    @Transactional
    public void disable(UUID tripId, UUID personId, UserEntity viewer) {
        authorizationService.requireAdmin(tripId, viewer);
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Personne introuvable."));
        if (!person.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette personne n'appartient pas au voyage indiqué.");
        }
        person.setActive(false);
        personRepository.save(person);
        auditService.log(person.getTrip(), viewer, AuditAction.PERSON_DISABLED, "PERSON", person.getId(), "Personne désactivée : " + person.getName());
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> list(UUID tripId, UserEntity viewer) {
        authorizationService.requireReadable(tripId, viewer);
        return personRepository.findByTripIdOrderByNameAsc(tripId).stream()
                .map(person -> mapper.toPersonResponse(person, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonEntity getRequiredInTrip(UUID tripId, UUID personId) {
        PersonEntity person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Personne introuvable."));
        if (!person.getTrip().getId().equals(tripId)) {
            throw new IllegalArgumentException("Cette personne n'appartient pas au voyage indiqué.");
        }
        return person;
    }

    private boolean canEditFinancialProfile(PersonEntity person, UserEntity viewer) {
        if (person.getLinkedUser() == null) {
            return true;
        }
        return viewer != null && person.getLinkedUser().getId().equals(viewer.getId());
    }

    private void applyCreate(PersonEntity person, PersonCreateRequest request, String normalizedName) {
        person.setName(request.name().trim().replaceAll("\\s+", " "));
        person.setNormalizedName(normalizedName);
        applyFinancialFields(person, request.weightMode(), request.advancedLivingRest(), request.livingRest(), request.netIncomeAfterTax(), request.rent(), request.credits(), request.fixedCharges(), request.transport(), request.insurance(), request.otherMandatoryExpenses(), request.menstrualProtection(), request.livingRestPublic(), request.vegetarian(), request.noAlcohol(), request.customConstraints());
        replacePresencePeriods(person, request.presencePeriods());
        validatePerson(person);
    }

    private void applyUpdate(PersonEntity person, PersonUpdateRequest request, String normalizedName, boolean canEditFinancialProfile) {
        person.setName(request.name().trim().replaceAll("\\s+", " "));
        person.setNormalizedName(normalizedName);
        if (canEditFinancialProfile) {
            applyFinancialFields(person, request.weightMode(), request.advancedLivingRest(), request.livingRest(), request.netIncomeAfterTax(), request.rent(), request.credits(), request.fixedCharges(), request.transport(), request.insurance(), request.otherMandatoryExpenses(), request.menstrualProtection(), request.livingRestPublic(), request.vegetarian(), request.noAlcohol(), request.customConstraints());
        }
        person.setActive(request.active());
        replacePresencePeriods(person, request.presencePeriods());
        validatePerson(person);
    }

    private void applyFinancialFields(PersonEntity person,
                                      WeightMode weightMode,
                                      boolean advancedLivingRest,
                                      BigDecimal livingRest,
                                      BigDecimal netIncomeAfterTax,
                                      BigDecimal rent,
                                      BigDecimal credits,
                                      BigDecimal fixedCharges,
                                      BigDecimal transport,
                                      BigDecimal insurance,
                                      BigDecimal otherMandatoryExpenses,
                                      BigDecimal menstrualProtection,
                                      boolean livingRestPublic,
                                      boolean vegetarian,
                                      boolean noAlcohol,
                                      Set<String> customConstraints) {
        person.setWeightMode(weightMode == null ? WeightMode.LIVING_REST : weightMode);
        person.setAdvancedLivingRest(advancedLivingRest);
        person.setNetIncomeAfterTax(mapper.money(netIncomeAfterTax));
        person.setRent(mapper.money(rent));
        person.setCredits(mapper.money(credits));
        person.setFixedCharges(mapper.money(fixedCharges));
        person.setTransport(mapper.money(transport));
        person.setInsurance(mapper.money(insurance));
        person.setOtherMandatoryExpenses(mapper.money(otherMandatoryExpenses));
        person.setMenstrualProtection(mapper.money(menstrualProtection));
        person.setLivingRest(computeLivingRest(advancedLivingRest, livingRest, person));
        person.setLivingRestPublic(livingRestPublic);
        person.setVegetarian(vegetarian);
        person.setNoAlcohol(noAlcohol);
        replaceCustomConstraints(person, customConstraints);
    }

    private void replaceCustomConstraints(PersonEntity person, Set<String> constraints) {
        if (person.getCustomConstraints() == null) {
            person.setCustomConstraints(new LinkedHashSet<>());
        }
        person.getCustomConstraints().clear();
        person.getCustomConstraints().addAll(normalizeCustomConstraintsForTrip(person.getTrip(), constraints));
    }

    private Set<String> normalizeCustomConstraintsForTrip(TripEntity trip, Set<String> constraints) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (constraints == null) {
            return normalized;
        }
        Set<String> allowedKeys = new LinkedHashSet<>();
        if (trip.getCustomConstraints() != null) {
            trip.getCustomConstraints().forEach(value -> allowedKeys.add(ConstraintNameUtils.key(value)));
        }
        Set<String> seenKeys = new LinkedHashSet<>();
        for (String rawConstraint : constraints) {
            String displayName = ConstraintNameUtils.canonicalDisplayName(rawConstraint);
            String key = ConstraintNameUtils.key(displayName);
            if (key.isBlank()) {
                continue;
            }
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("La contrainte \"" + displayName + "\" n'est pas déclarée dans ce voyage.");
            }
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("Une contrainte personnalisée est renseignée plusieurs fois.");
            }
            normalized.add(displayName);
        }
        return normalized;
    }

    private BigDecimal computeLivingRest(boolean advanced, BigDecimal requestLivingRest, PersonEntity person) {
        if (!advanced) {
            return mapper.money(requestLivingRest);
        }
        return mapper.money(person.getNetIncomeAfterTax())
                .subtract(mapper.money(person.getRent()))
                .subtract(mapper.money(person.getCredits()))
                .subtract(mapper.money(person.getFixedCharges()))
                .subtract(mapper.money(person.getTransport()))
                .subtract(mapper.money(person.getInsurance()))
                .subtract(mapper.money(person.getOtherMandatoryExpenses()))
                .subtract(mapper.money(person.getMenstrualProtection()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void replacePresencePeriods(PersonEntity person, List<PresencePeriodRequest> periods) {
        person.getPresencePeriods().clear();
        List<PresencePeriodRequest> normalizedPeriods = validatePresencePeriods(person.getTrip(), periods);
        for (PresencePeriodRequest request : normalizedPeriods) {
            PresencePeriodEntity period = new PresencePeriodEntity();
            period.setPerson(person);
            period.setStartDate(request.startDate());
            period.setEndDate(request.endDate());
            person.getPresencePeriods().add(period);
        }
    }

    private List<PresencePeriodRequest> validatePresencePeriods(TripEntity trip, List<PresencePeriodRequest> periods) {
        if (periods == null || periods.isEmpty()) {
            throw new IllegalArgumentException("Au moins une période de présence est obligatoire.");
        }

        for (PresencePeriodRequest period : periods) {
            if (period.startDate() == null || period.endDate() == null) {
                throw new IllegalArgumentException("Chaque période de présence doit avoir une date de début et une date de fin.");
            }
        }

        List<PresencePeriodRequest> sortedPeriods = new ArrayList<>(periods);
        sortedPeriods.sort(Comparator.comparing(PresencePeriodRequest::startDate));

        PresencePeriodRequest previous = null;
        for (PresencePeriodRequest period : sortedPeriods) {
            if (period.startDate().isAfter(period.endDate())) {
                throw new IllegalArgumentException("Une période de présence commence après sa date de fin.");
            }
            if (trip.getStartDate() != null && period.startDate().isBefore(trip.getStartDate())) {
                throw new IllegalArgumentException("Une période de présence commence avant le début du voyage.");
            }
            if (trip.getEndDate() != null && period.endDate().isAfter(trip.getEndDate())) {
                throw new IllegalArgumentException("Une période de présence se termine après la fin du voyage.");
            }
            if (previous != null && !period.startDate().isAfter(previous.endDate())) {
                throw new IllegalArgumentException("Les périodes de présence d'une même personne ne doivent pas se chevaucher.");
            }
            previous = period;
        }

        return sortedPeriods;
    }

    private void validatePerson(PersonEntity person) {
        if (person.getWeightMode() != WeightMode.AVERAGE && mapper.money(person.getLivingRest()).signum() <= 0) {
            throw new IllegalArgumentException("Le reste à vivre doit être strictement positif sauf si la personne utilise le poids moyen.");
        }
    }
}
