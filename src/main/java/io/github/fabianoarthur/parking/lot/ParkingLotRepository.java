package io.github.fabianoarthur.parking.lot;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, UUID> {}
