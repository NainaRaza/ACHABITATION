package fr.achabitation.api.controller;

import fr.achabitation.api.dto.PersonDtos.CurrentUserPersonCreateRequest;
import fr.achabitation.api.dto.PersonDtos.PersonCreateRequest;
import fr.achabitation.api.dto.PersonDtos.LinkGuestRequest;
import fr.achabitation.api.dto.PersonDtos.PersonResponse;
import fr.achabitation.api.dto.PersonDtos.PersonUpdateRequest;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.PersonService;
import fr.achabitation.infrastructure.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/persons")
public class PersonController {
    private final PersonService personService;
    private final AuthContextService authContextService;

    public PersonController(PersonService personService, AuthContextService authContextService) {
        this.personService = personService;
        this.authContextService = authContextService;
    }

    @PostMapping
    public PersonResponse create(@PathVariable UUID tripId, @Valid @RequestBody PersonCreateRequest request, HttpServletRequest httpRequest) {
        return personService.create(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @PostMapping("/current-user")
    public PersonResponse createForCurrentUser(@PathVariable UUID tripId, @Valid @RequestBody CurrentUserPersonCreateRequest request, HttpServletRequest httpRequest) {
        return personService.createForCurrentUser(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @GetMapping
    public List<PersonResponse> list(@PathVariable UUID tripId, HttpServletRequest httpRequest) {
        return personService.list(tripId, authContextService.requiredUser(httpRequest));
    }

    @PutMapping("/{personId}")
    public PersonResponse update(@PathVariable UUID tripId, @PathVariable UUID personId, @Valid @RequestBody PersonUpdateRequest request, HttpServletRequest httpRequest) {
        return personService.update(tripId, personId, request, authContextService.requiredUser(httpRequest));
    }

    @PostMapping("/{personId}/link-current-user")
    public PersonResponse linkCurrentUser(@PathVariable UUID tripId, @PathVariable UUID personId, @RequestBody(required = false) LinkGuestRequest request, HttpServletRequest httpRequest) {
        UserEntity user = authContextService.requiredUser(httpRequest);
        boolean applyProfileToGuest = request != null && request.applyProfileToGuest();
        return personService.linkToCurrentUser(tripId, personId, user, applyProfileToGuest);
    }

    @DeleteMapping("/{personId}")
    public void disable(@PathVariable UUID tripId, @PathVariable UUID personId, HttpServletRequest httpRequest) {
        personService.disable(tripId, personId, authContextService.requiredUser(httpRequest));
    }
}
