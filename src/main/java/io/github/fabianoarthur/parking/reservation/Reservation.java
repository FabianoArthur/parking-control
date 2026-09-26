package io.github.fabianoarthur.parking.reservation;

import io.github.fabianoarthur.parking.common.ConflictException;
import io.github.fabianoarthur.parking.spot.ParkingSpot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "spot_id", nullable = false)
  private ParkingSpot spot;

  @Column(name = "license_plate", nullable = false, length = 7)
  private String licensePlate;

  @Column(name = "starts_at", nullable = false)
  private Instant startsAt;

  @Column(name = "ends_at", nullable = false)
  private Instant endsAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Reservation() {}

  public Reservation(
      ParkingSpot spot, String licensePlate, Instant startsAt, Instant endsAt, Instant createdAt) {
    this.spot = spot;
    this.licensePlate = licensePlate;
    this.startsAt = startsAt;
    this.endsAt = endsAt;
    this.status = ReservationStatus.ACTIVE;
    this.createdAt = createdAt;
  }

  public void cancel(Instant now) {
    if (status != ReservationStatus.ACTIVE || !now.isBefore(startsAt)) {
      throw new ConflictException(
          "RESERVATION_NOT_CANCELLABLE",
          "Only active reservations that have not started can be cancelled");
    }
    status = ReservationStatus.CANCELLED;
  }

  public void fulfill() {
    status = ReservationStatus.FULFILLED;
  }

  public UUID getId() {
    return id;
  }

  public ParkingSpot getSpot() {
    return spot;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public ReservationStatus getStatus() {
    return status;
  }
}
