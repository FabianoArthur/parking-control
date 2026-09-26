package io.github.fabianoarthur.parking.lot;

import io.github.fabianoarthur.parking.lot.LotDtos.AvailabilityResponse;
import io.github.fabianoarthur.parking.lot.LotDtos.CreateLotRequest;
import io.github.fabianoarthur.parking.lot.LotDtos.LotResponse;
import io.github.fabianoarthur.parking.lot.LotDtos.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lots")
@Tag(name = "Lots", description = "Parking lots, their pricing and live availability")
public class LotController {

  private final LotService service;

  public LotController(LotService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Create a parking lot with its pricing policy")
  public ResponseEntity<LotResponse> create(@Valid @RequestBody CreateLotRequest request) {
    LotResponse body = LotResponse.of(service.create(request));
    return ResponseEntity.created(URI.create("/api/v1/lots/" + body.id())).body(body);
  }

  @GetMapping
  @Operation(summary = "List parking lots (paged, max 100 per page)")
  public Page<LotResponse> list(
      @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return service.list(pageable).map(LotResponse::of);
  }

  @GetMapping("/{lotId}")
  @Operation(summary = "Get a parking lot")
  public LotResponse get(@PathVariable UUID lotId) {
    return LotResponse.of(service.get(lotId));
  }

  @GetMapping("/{lotId}/availability")
  @Operation(summary = "Free, occupied and reserved spots right now, per spot type")
  public AvailabilityResponse availability(@PathVariable UUID lotId) {
    return service.availability(lotId);
  }

  @GetMapping("/{lotId}/quote")
  @Operation(summary = "Price a hypothetical stay with this lot's pricing policy")
  public QuoteResponse quote(
      @PathVariable UUID lotId,
      @Parameter(example = "2026-03-02T08:00:00Z") @RequestParam Instant entryAt,
      @Parameter(example = "2026-03-02T10:30:00Z") @RequestParam Instant exitAt) {
    return service.quote(lotId, entryAt, exitAt);
  }
}
