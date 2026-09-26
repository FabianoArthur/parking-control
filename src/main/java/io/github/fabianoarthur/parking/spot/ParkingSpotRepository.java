package io.github.fabianoarthur.parking.spot;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, UUID> {

  Optional<ParkingSpot> findByLotIdAndCode(UUID lotId, String code);

  boolean existsByLotIdAndCode(UUID lotId, String code);

  List<ParkingSpot> findByLotIdOrderByCode(UUID lotId);

  /** Locks the spot row so concurrent reservations on it are checked one at a time. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ParkingSpot s where s.lot.id = :lotId and s.code = :code")
  Optional<ParkingSpot> findForUpdate(@Param("lotId") UUID lotId, @Param("code") String code);

  @Query(
      """
      select s from ParkingSpot s
      where s.lot.id = :lotId
        and (:type is null or s.type = :type)
        and (:occupied is null or s.occupied = :occupied)
      """)
  Page<ParkingSpot> search(
      @Param("lotId") UUID lotId,
      @Param("type") SpotType type,
      @Param("occupied") Boolean occupied,
      Pageable pageable);

  @Query(
      """
      select s from ParkingSpot s
      where s.lot.id = :lotId and s.occupied = false
        and (:type is null or s.type = :type)
      order by s.code
      """)
  List<ParkingSpot> findFree(@Param("lotId") UUID lotId, @Param("type") SpotType type);
}
