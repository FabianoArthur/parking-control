package io.github.fabianoarthur.parking.spot;

import io.github.fabianoarthur.parking.lot.ParkingLot;
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
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "parking_spot")
public class ParkingSpot {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lot_id", nullable = false)
  private ParkingLot lot;

  @Column(nullable = false, length = 20)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SpotType type;

  @Column(nullable = false)
  private boolean occupied;

  /** Optimistic lock: two cars racing for the same spot cannot both win. */
  @Version private long version;

  protected ParkingSpot() {}

  public ParkingSpot(ParkingLot lot, String code, SpotType type) {
    this.lot = lot;
    this.code = code;
    this.type = type;
  }

  public void occupy() {
    this.occupied = true;
  }

  public void release() {
    this.occupied = false;
  }

  public UUID getId() {
    return id;
  }

  public ParkingLot getLot() {
    return lot;
  }

  public String getCode() {
    return code;
  }

  public SpotType getType() {
    return type;
  }

  public boolean isOccupied() {
    return occupied;
  }
}
