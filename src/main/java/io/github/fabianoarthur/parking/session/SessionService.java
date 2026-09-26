package io.github.fabianoarthur.parking.session;

import io.github.fabianoarthur.parking.common.BusinessRuleException;
import io.github.fabianoarthur.parking.common.ConflictException;
import io.github.fabianoarthur.parking.common.LicensePlate;
import io.github.fabianoarthur.parking.common.NotFoundException;
import io.github.fabianoarthur.parking.lot.LotService;
import io.github.fabianoarthur.parking.pricing.PriceCalculator;
import io.github.fabianoarthur.parking.reservation.Reservation;
import io.github.fabianoarthur.parking.reservation.ReservationRepository;
import io.github.fabianoarthur.parking.spot.ParkingSpot;
import io.github.fabianoarthur.parking.spot.ParkingSpotRepository;
import io.github.fabianoarthur.parking.spot.SpotType;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

  private final LotService lots;
  private final ParkingSpotRepository spots;
  private final ReservationRepository reservations;
  private final ParkingSessionRepository sessions;
  private final Clock clock;

  public SessionService(
      LotService lots,
      ParkingSpotRepository spots,
      ReservationRepository reservations,
      ParkingSessionRepository sessions,
      Clock clock) {
    this.lots = lots;
    this.spots = spots;
    this.reservations = reservations;
    this.sessions = sessions;
    this.clock = clock;
  }

  /**
   * Parks a car. A car with a reservation in effect goes to its reserved spot; otherwise it takes
   * the requested spot, or the first free spot (of the requested type) nobody has reserved now.
   */
  @Transactional
  public ParkingSession checkIn(UUID lotId, String rawPlate, String spotCode, SpotType spotType) {
    lots.get(lotId);
    LicensePlate plate = LicensePlate.of(rawPlate);
    Instant now = clock.instant();
    if (sessions.existsByOpenPlate(plate.value())) {
      throw new ConflictException("PLATE_ALREADY_PARKED", plate + " is already parked");
    }

    Optional<Reservation> reservation = reservations.inEffectForPlate(lotId, plate.value(), now);
    ParkingSpot spot;
    if (reservation.isPresent()) {
      spot = reservation.get().getSpot();
      if (spotCode != null && !spot.getCode().equalsIgnoreCase(spotCode)) {
        throw new BusinessRuleException(
            "RESERVED_ELSEWHERE", plate + " has a reservation for spot " + spot.getCode());
      }
    } else if (spotCode != null) {
      String code = spotCode.toUpperCase(Locale.ROOT);
      spot =
          spots
              .findByLotIdAndCode(lotId, code)
              .orElseThrow(() -> new NotFoundException("Spot " + code + " was not found"));
      if (reservations.inEffectForSpot(spot.getId(), now).isPresent()) {
        throw new ConflictException("SPOT_RESERVED", "Spot " + code + " is reserved right now");
      }
    } else {
      Set<UUID> reservedNow = reservations.spotIdsReservedAt(lotId, now);
      spot =
          spots.findFree(lotId, spotType).stream()
              .filter(s -> !reservedNow.contains(s.getId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new ConflictException(
                          "NO_SPOT_AVAILABLE",
                          spotType == null
                              ? "The lot is full"
                              : "No " + spotType + " spot is available"));
    }

    if (spot.isOccupied()) {
      throw new ConflictException("SPOT_OCCUPIED", "Spot " + spot.getCode() + " is occupied");
    }
    spot.occupy();
    reservation.ifPresent(Reservation::fulfill);
    return sessions.save(new ParkingSession(spot, plate.value(), reservation.orElse(null), now));
  }

  @Transactional
  public ParkingSession checkOut(UUID sessionId) {
    ParkingSession session = get(sessionId);
    Instant now = clock.instant();
    ParkingSpot spot = session.getSpot();
    long amount =
        PriceCalculator.priceCents(spot.getLot().pricingPolicy(), session.getEntryAt(), now);
    session.close(now, amount);
    spot.release();
    return session;
  }

  @Transactional(readOnly = true)
  public ParkingSession get(UUID sessionId) {
    return sessions
        .findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Parking session", sessionId));
  }
}
