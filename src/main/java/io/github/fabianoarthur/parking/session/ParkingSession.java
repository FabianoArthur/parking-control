package io.github.fabianoarthur.parking.session;

import io.github.fabianoarthur.parking.common.ConflictException;
import io.github.fabianoarthur.parking.reservation.Reservation;
import io.github.fabianoarthur.parking.spot.ParkingSpot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One stay of one car: opened at check-in, closed and priced at check-out. */
@Entity
@Table(name = "parking_session")
public class ParkingSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "spot_id", nullable = false)
  private ParkingSpot spot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id")
  private Reservation reservation;

  @Column(name = "license_plate", nullable = false, length = 7)
  private String licensePlate;

  /** Same as {@link #licensePlate} while open, null once closed (backed by a UNIQUE key). */
  @Column(name = "open_plate", length = 7)
  private String openPlate;

  @Column(name = "entry_at", nullable = false)
  private Instant entryAt;

  @Column(name = "exit_at")
  private Instant exitAt;

  @Column(name = "amount_cents")
  private Long amountCents;

  protected ParkingSession() {}

  public ParkingSession(
      ParkingSpot spot, String licensePlate, Reservation reservation, Instant entryAt) {
    this.spot = spot;
    this.licensePlate = licensePlate;
    this.openPlate = licensePlate;
    this.reservation = reservation;
    this.entryAt = entryAt;
  }

  public void close(Instant exitAt, long amountCents) {
    if (!isOpen()) {
      throw new ConflictException("SESSION_ALREADY_CLOSED", "This session is already closed");
    }
    this.exitAt = exitAt;
    this.amountCents = amountCents;
    this.openPlate = null;
  }

  public boolean isOpen() {
    return exitAt == null;
  }

  public UUID getId() {
    return id;
  }

  public ParkingSpot getSpot() {
    return spot;
  }

  public Reservation getReservation() {
    return reservation;
  }

  public String getLicensePlate() {
    return licensePlate;
  }

  public Instant getEntryAt() {
    return entryAt;
  }

  public Instant getExitAt() {
    return exitAt;
  }

  public Long getAmountCents() {
    return amountCents;
  }
}
