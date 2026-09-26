package io.github.fabianoarthur.parking.reservation;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

  /** Half-open windows: [startsAt, endsAt). Back-to-back reservations do not overlap. */
  @Query(
      """
      select count(r) > 0 from Reservation r
      where r.spot.id = :spotId and r.status = 'ACTIVE'
        and r.startsAt < :endsAt and r.endsAt > :startsAt
      """)
  boolean spotHasOverlap(
      @Param("spotId") UUID spotId,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt);

  @Query(
      """
      select count(r) > 0 from Reservation r
      where r.spot.lot.id = :lotId and r.licensePlate = :plate and r.status = 'ACTIVE'
        and r.startsAt < :endsAt and r.endsAt > :startsAt
      """)
  boolean plateHasOverlap(
      @Param("lotId") UUID lotId,
      @Param("plate") String plate,
      @Param("startsAt") Instant startsAt,
      @Param("endsAt") Instant endsAt);

  @Query(
      """
      select r from Reservation r
      where r.spot.id = :spotId and r.status = 'ACTIVE'
        and r.startsAt <= :at and r.endsAt > :at
      """)
  Optional<Reservation> inEffectForSpot(@Param("spotId") UUID spotId, @Param("at") Instant at);

  @Query(
      """
      select r from Reservation r join fetch r.spot
      where r.spot.lot.id = :lotId and r.licensePlate = :plate and r.status = 'ACTIVE'
        and r.startsAt <= :at and r.endsAt > :at
      """)
  Optional<Reservation> inEffectForPlate(
      @Param("lotId") UUID lotId, @Param("plate") String plate, @Param("at") Instant at);

  @Query(
      """
      select r.spot.id from Reservation r
      where r.spot.lot.id = :lotId and r.status = 'ACTIVE'
        and r.startsAt <= :at and r.endsAt > :at
      """)
  Set<UUID> spotIdsReservedAt(@Param("lotId") UUID lotId, @Param("at") Instant at);
}
