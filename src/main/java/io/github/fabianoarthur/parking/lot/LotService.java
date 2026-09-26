package io.github.fabianoarthur.parking.lot;

import io.github.fabianoarthur.parking.common.BusinessRuleException;
import io.github.fabianoarthur.parking.common.NotFoundException;
import io.github.fabianoarthur.parking.lot.LotDtos.AvailabilityResponse;
import io.github.fabianoarthur.parking.lot.LotDtos.CreateLotRequest;
import io.github.fabianoarthur.parking.lot.LotDtos.QuoteResponse;
import io.github.fabianoarthur.parking.lot.LotDtos.TypeAvailability;
import io.github.fabianoarthur.parking.pricing.PriceCalculator;
import io.github.fabianoarthur.parking.pricing.PricingPolicy;
import io.github.fabianoarthur.parking.reservation.ReservationRepository;
import io.github.fabianoarthur.parking.spot.ParkingSpot;
import io.github.fabianoarthur.parking.spot.ParkingSpotRepository;
import io.github.fabianoarthur.parking.spot.SpotType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotService {

  private final ParkingLotRepository lots;
  private final ParkingSpotRepository spots;
  private final ReservationRepository reservations;
  private final Clock clock;

  public LotService(
      ParkingLotRepository lots,
      ParkingSpotRepository spots,
      ReservationRepository reservations,
      Clock clock) {
    this.lots = lots;
    this.spots = spots;
    this.reservations = reservations;
    this.clock = clock;
  }

  @Transactional
  public ParkingLot create(CreateLotRequest request) {
    PricingPolicy policy;
    try {
      policy = request.pricing().toPolicy();
    } catch (IllegalArgumentException e) {
      throw new BusinessRuleException("INVALID_PRICING", e.getMessage());
    }
    return lots.save(new ParkingLot(request.name().strip(), policy, clock.instant()));
  }

  @Transactional(readOnly = true)
  public ParkingLot get(UUID id) {
    return lots.findById(id).orElseThrow(() -> new NotFoundException("Parking lot", id));
  }

  @Transactional(readOnly = true)
  public Page<ParkingLot> list(Pageable pageable) {
    return lots.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public QuoteResponse quote(UUID lotId, Instant entryAt, Instant exitAt) {
    if (exitAt.isBefore(entryAt)) {
      throw new BusinessRuleException("INVALID_PERIOD", "exitAt must not be before entryAt");
    }
    long amount = PriceCalculator.priceCents(get(lotId).pricingPolicy(), entryAt, exitAt);
    return new QuoteResponse(entryAt, exitAt, amount);
  }

  @Transactional(readOnly = true)
  public AvailabilityResponse availability(UUID lotId) {
    get(lotId);
    Instant now = clock.instant();
    List<ParkingSpot> all = spots.findByLotIdOrderByCode(lotId);
    Set<UUID> reservedNow = reservations.spotIdsReservedAt(lotId, now);

    List<TypeAvailability> byType = new ArrayList<>();
    for (SpotType type : SpotType.values()) {
      List<ParkingSpot> ofType = all.stream().filter(s -> s.getType() == type).toList();
      if (ofType.isEmpty()) {
        continue;
      }
      long occupied = ofType.stream().filter(ParkingSpot::isOccupied).count();
      long reserved =
          ofType.stream().filter(s -> !s.isOccupied() && reservedNow.contains(s.getId())).count();
      byType.add(
          new TypeAvailability(
              type, ofType.size(), occupied, reserved, ofType.size() - occupied - reserved));
    }
    return new AvailabilityResponse(
        lotId,
        now,
        byType.stream().mapToLong(TypeAvailability::total).sum(),
        byType.stream().mapToLong(TypeAvailability::occupied).sum(),
        byType.stream().mapToLong(TypeAvailability::reserved).sum(),
        byType.stream().mapToLong(TypeAvailability::available).sum(),
        byType);
  }
}
