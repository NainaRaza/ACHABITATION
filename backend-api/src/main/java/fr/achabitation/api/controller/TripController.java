package fr.achabitation.api.controller;

import fr.achabitation.api.dto.TripDtos.JoinTripByCodeRequest;
import fr.achabitation.api.dto.TripDtos.JoinTripRequest;
import fr.achabitation.api.dto.TripDtos.TripConstraintUpdateRequest;
import fr.achabitation.api.dto.TripDtos.TripCreateRequest;
import fr.achabitation.api.dto.TripDtos.TripInvitationCreateRequest;
import fr.achabitation.api.dto.TripDtos.TripInvitationResponse;
import fr.achabitation.api.dto.TripDtos.TripResponse;
import fr.achabitation.application.AuthContextService;
import fr.achabitation.application.TripService;
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
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripService tripService;
    private final AuthContextService authContextService;

    public TripController(TripService tripService, AuthContextService authContextService) {
        this.tripService = tripService;
        this.authContextService = authContextService;
    }

    @PostMapping
    public TripResponse create(@Valid @RequestBody TripCreateRequest request, HttpServletRequest httpRequest) {
        return tripService.create(request, authContextService.requiredUser(httpRequest));
    }

    @GetMapping
    public List<TripResponse> list(HttpServletRequest httpRequest) {
        return tripService.list(authContextService.requiredUser(httpRequest));
    }

    @PostMapping("/join-by-code")
    public TripResponse joinByCode(@Valid @RequestBody JoinTripByCodeRequest request, HttpServletRequest httpRequest) {
        return tripService.joinByCode(request, authContextService.requiredUser(httpRequest));
    }

    @PutMapping("/{tripId}/constraints")
    public TripResponse updateConstraints(@PathVariable UUID tripId, @Valid @RequestBody TripConstraintUpdateRequest request, HttpServletRequest httpRequest) {
        return tripService.updateConstraints(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @PostMapping("/{tripId}/join")
    public TripResponse join(@PathVariable UUID tripId, @Valid @RequestBody(required = false) JoinTripRequest request, HttpServletRequest httpRequest) {
        return tripService.join(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @PostMapping("/{tripId}/invitations")
    public TripInvitationResponse createInvitation(@PathVariable UUID tripId, @Valid @RequestBody(required = false) TripInvitationCreateRequest request, HttpServletRequest httpRequest) {
        return tripService.createInvitation(tripId, request, authContextService.requiredUser(httpRequest));
    }

    @GetMapping("/{tripId}/invitations")
    public List<TripInvitationResponse> listInvitations(@PathVariable UUID tripId, HttpServletRequest httpRequest) {
        return tripService.listInvitations(tripId, authContextService.requiredUser(httpRequest));
    }

    @DeleteMapping("/{tripId}/invitations/{invitationId}")
    public void revokeInvitation(@PathVariable UUID tripId, @PathVariable UUID invitationId, HttpServletRequest httpRequest) {
        tripService.revokeInvitation(tripId, invitationId, authContextService.requiredUser(httpRequest));
    }
}
