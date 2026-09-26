package io.github.fabianoarthur.parking.session;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

  boolean existsByOpenPlate(String openPlate);
}
