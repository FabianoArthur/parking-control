package io.github.fabianoarthur.parking.session;

import io.github.fabianoarthur.parking.spot.SpotType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Sessions", description = "Check-in and check-out of cars")
public class SessionController {

  private final SessionService service;

  public SessionController(SessionService service) {
    this.service = service;
  }

  @Schema(description = "Give spotCode, spotType or neither (any free spot)")
  public record CheckInRequest(
      @Schema(example = "ABC1D23") @NotBlank String licensePlate,
      @Schema(example = "A-01", nullable = true) String spotCode,
      @Schema(nullable = true) SpotType spotType) {}

  public enum SessionStatus {
    OPEN,
    CLOSED
  }

  public record SessionResponse(
      UUID id,
      UUID lotId,
      String spotCode,
      String licensePlate,
      UUID reservationId,
      Instant entryAt,
      Instant exitAt,
      Long amountCents,
      SessionStatus status) {
    static SessionResponse of(ParkingSession s) {
      return new SessionResponse(
          s.getId(),
          s.getSpot().getLot().getId(),
          s.getSpot().getCode(),
          s.getLicensePlate(),
          s.getReservation() == null ? null : s.getReservation().getId(),
          s.getEntryAt(),
          s.getExitAt(),
          s.getAmountCents(),
          s.isOpen() ? SessionStatus.OPEN : SessionStatus.CLOSED);
    }
  }

  @PostMapping("/lots/{lotId}/sessions")
  @Operation(summary = "Check a car in")
  public ResponseEntity<SessionResponse> checkIn(
      @PathVariable UUID lotId, @Valid @RequestBody CheckInRequest request) {
    ParkingSession session =
        service.checkIn(lotId, request.licensePlate(), request.spotCode(), request.spotType());
    return ResponseEntity.created(URI.create("/api/v1/sessions/" + session.getId()))
        .body(SessionResponse.of(session));
  }

  @PostMapping("/sessions/{id}/checkout")
  @Operation(summary = "Check a car out and charge the stay")
  public SessionResponse checkOut(@PathVariable UUID id) {
    return SessionResponse.of(service.checkOut(id));
  }

  @GetMapping("/sessions/{id}")
  @Transactional(readOnly = true)
  @Operation(summary = "Get a parking session")
  public SessionResponse get(@PathVariable UUID id) {
    return SessionResponse.of(service.get(id));
  }
}
