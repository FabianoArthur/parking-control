package io.github.fabianoarthur.parking.lot;

import io.github.fabianoarthur.parking.pricing.PricingPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parking_lot")
public class ParkingLot {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "grace_period_minutes", nullable = false)
  private int gracePeriodMinutes;

  @Column(name = "first_hour_cents", nullable = false)
  private long firstHourCents;

  @Column(name = "fraction_minutes", nullable = false)
  private int fractionMinutes;

  @Column(name = "fraction_cents", nullable = false)
  private long fractionCents;

  @Column(name = "daily_cap_cents", nullable = false)
  private long dailyCapCents;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ParkingLot() {}

  public ParkingLot(String name, PricingPolicy policy, Instant createdAt) {
    this.name = name;
    this.gracePeriodMinutes = Math.toIntExact(policy.gracePeriod().toMinutes());
    this.firstHourCents = policy.firstHourCents();
    this.fractionMinutes = Math.toIntExact(policy.fraction().toMinutes());
    this.fractionCents = policy.fractionCents();
    this.dailyCapCents = policy.dailyCapCents();
    this.createdAt = createdAt;
  }

  public PricingPolicy pricingPolicy() {
    return new PricingPolicy(
        Duration.ofMinutes(gracePeriodMinutes),
        firstHourCents,
        Duration.ofMinutes(fractionMinutes),
        fractionCents,
        dailyCapCents);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
