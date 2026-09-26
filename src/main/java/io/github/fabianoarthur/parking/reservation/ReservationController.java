package io.github.fabianoarthur.parking.reservation;

import io.github.fabianoarthur.parking.common.BusinessRuleException;
import io.github.fabianoarthur.parking.common.ConflictException;
import io.github.fabianoarthur.parking.common.LicensePlate;
import io.github.fabianoarthur.parking.common.NotFoundException;
import io.github.fabianoarthur.parking.lot.LotService;
import io.github.fabianoarthur.parking.spot.ParkingSpot;
import io.github.fabianoarthur.parking.spot.ParkingSpotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reservations", description = "Hold a spot for a car during a time window")
public class ReservationController {

  static final Duration MAX_LENGTH = Duration.ofHours(24);

  private final LotService lots;
  private final ParkingSpotRepository spots;
  private final ReservationRepository reservations;
  private final Clock clock;

  public ReservationController(
      LotService lots,
      ParkingSpotRepository spots,
      ReservationRepository reservations,
      Clock clock) {
    this.lots = lots;
    this.spots = spots;
    this.reservations = reservations;
    this.clock = clock;
  }

  public record CreateReservationRequest(
      @Schema(example = "E-01") @NotBlank String spotCode,
      @Schema(example = "ABC1D23") @NotBlank String licensePlate,
      @Schema(example = "2026-03-02T09:00:00Z") @NotNull Instant startsAt,
      @Schema(example = "2026-03-02T12:00:00Z") @NotNull Instant endsAt) {}

  public record ReservationResponse(
      UUID id,
      UUID lotId,
      String spotCode,
      String licensePlate,
      Instant startsAt,
      Instant endsAt,
      ReservationStatus status) {
    static ReservationResponse of(Reservation r) {
      return new ReservationResponse(
          r.getId(),
          r.getSpot().getLot().getId(),
          r.getSpot().getCode(),
          r.getLicensePlate(),
          r.getStartsAt(),
          r.getEndsAt(),
          r.getStatus());
    }
  }

  @PostMapping("/lots/{lotId}/reservations")
  @Transactional
  @Operation(summary = "Reserve a spot for a time window (max 24h, must start in the future)")
  public ResponseEntity<ReservationResponse> create(
      @PathVariable UUID lotId, @Valid @RequestBody CreateReservationRequest request) {
    lots.get(lotId);
    LicensePlate plate = LicensePlate.of(request.licensePlate());
    Instant now = clock.instant();
    if (!request.startsAt().isAfter(now)) {
      throw new BusinessRuleException("INVALID_PERIOD", "startsAt must be in the future");
    }
    if (!request.endsAt().isAfter(request.startsAt())) {
      throw new BusinessRuleException("INVALID_PERIOD", "endsAt must be after startsAt");
    }
    if (Duration.between(request.startsAt(), request.endsAt()).compareTo(MAX_LENGTH) > 0) {
      throw new BusinessRuleException("INVALID_PERIOD", "a reservation lasts at most 24 hours");
    }
    String code = request.spotCode().toUpperCase(Locale.ROOT);
    ParkingSpot spot =
        spots
            .findForUpdate(lotId, code)
            .orElseThrow(() -> new NotFoundException("Spot " + code + " was not found"));
    if (reservations.spotHasOverlap(spot.getId(), request.startsAt(), request.endsAt())) {
      throw new ConflictException(
          "SPOT_ALREADY_RESERVED", "Spot " + code + " is already reserved in that window");
    }
    if (reservations.plateHasOverlap(lotId, plate.value(), request.startsAt(), request.endsAt())) {
      throw new ConflictException(
          "PLATE_ALREADY_HAS_RESERVATION",
          plate + " already has a reservation in this lot in that window");
    }
    Reservation saved =
        reservations.save(
            new Reservation(spot, plate.value(), request.startsAt(), request.endsAt(), now));
    return ResponseEntity.created(URI.create("/api/v1/reservations/" + saved.getId()))
        .body(ReservationResponse.of(saved));
  }

  @GetMapping("/reservations/{id}")
  @Transactional(readOnly = true)
  @Operation(summary = "Get a reservation")
  public ReservationResponse get(@PathVariable UUID id) {
    return ReservationResponse.of(find(id));
  }

  @DeleteMapping("/reservations/{id}")
  @Transactional
  @Operation(summary = "Cancel a reservation that has not started yet")
  public ResponseEntity<Void> cancel(@PathVariable UUID id) {
    find(id).cancel(clock.instant());
    return ResponseEntity.noContent().build();
  }

  private Reservation find(UUID id) {
    return reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation", id));
  }
}
