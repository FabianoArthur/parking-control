package io.github.fabianoarthur.parking.spot;

import io.github.fabianoarthur.parking.common.ConflictException;
import io.github.fabianoarthur.parking.lot.LotService;
import io.github.fabianoarthur.parking.lot.ParkingLot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots/{lotId}/spots")
@Tag(name = "Spots", description = "Spots inside a lot")
public class SpotController {

  private final LotService lots;
  private final ParkingSpotRepository spots;

  public SpotController(LotService lots, ParkingSpotRepository spots) {
    this.lots = lots;
    this.spots = spots;
  }

  public record CreateSpotRequest(
      @Schema(example = "A-01") @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{1,20}") String code,
      @NotNull SpotType type) {}

  public record SpotResponse(UUID id, String code, SpotType type, boolean occupied) {
    static SpotResponse of(ParkingSpot spot) {
      return new SpotResponse(spot.getId(), spot.getCode(), spot.getType(), spot.isOccupied());
    }
  }

  @PostMapping
  @Transactional
  @Operation(summary = "Add a spot to a lot (codes are unique per lot)")
  public ResponseEntity<SpotResponse> create(
      @PathVariable UUID lotId, @Valid @RequestBody CreateSpotRequest request) {
    ParkingLot lot = lots.get(lotId);
    String code = request.code().toUpperCase(java.util.Locale.ROOT);
    if (spots.existsByLotIdAndCode(lotId, code)) {
      throw new ConflictException("SPOT_CODE_TAKEN", "Spot " + code + " already exists");
    }
    SpotResponse body = SpotResponse.of(spots.save(new ParkingSpot(lot, code, request.type())));
    return ResponseEntity.created(URI.create("/api/v1/lots/" + lotId + "/spots/" + body.id()))
        .body(body);
  }

  @GetMapping
  @Transactional(readOnly = true)
  @Operation(summary = "List spots, optionally filtered by type and occupancy")
  public Page<SpotResponse> list(
      @PathVariable UUID lotId,
      @RequestParam(required = false) SpotType type,
      @RequestParam(required = false) Boolean occupied,
      @PageableDefault(size = 50, sort = "code", direction = Sort.Direction.ASC)
          Pageable pageable) {
    lots.get(lotId);
    return spots.search(lotId, type, occupied, pageable).map(SpotResponse::of);
  }
}
