package io.github.fabianoarthur.parking.lot;

import io.github.fabianoarthur.parking.pricing.PricingPolicy;
import io.github.fabianoarthur.parking.spot.SpotType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request and response bodies of the lot endpoints. */
public final class LotDtos {

  /** R$ 1,000,000.00: far above any real tariff, and far from overflowing a multi-year stay. */
  static final long MAX_CENTS = 100_000_000L;

  private LotDtos() {}

  public record CreateLotRequest(
      @Schema(example = "Downtown Garage") @NotBlank @Size(max = 100) String name,
      @NotNull @Valid PricingRequest pricing) {}

  @Schema(description = "Prices are integer cents")
  public record PricingRequest(
      @Schema(example = "15") @NotNull @Min(0) @Max(1440) Integer gracePeriodMinutes,
      @Schema(example = "1000") @NotNull @PositiveOrZero @Max(MAX_CENTS) Long firstHourCents,
      @Schema(example = "30") @NotNull @Min(1) @Max(1440) Integer fractionMinutes,
      @Schema(example = "300") @NotNull @PositiveOrZero @Max(MAX_CENTS) Long fractionCents,
      @Schema(example = "6000") @NotNull @PositiveOrZero @Max(MAX_CENTS) Long dailyCapCents) {

    PricingPolicy toPolicy() {
      return new PricingPolicy(
          Duration.ofMinutes(gracePeriodMinutes),
          firstHourCents,
          Duration.ofMinutes(fractionMinutes),
          fractionCents,
          dailyCapCents);
    }
  }

  public record PricingResponse(
      long gracePeriodMinutes,
      long firstHourCents,
      long fractionMinutes,
      long fractionCents,
      long dailyCapCents) {

    static PricingResponse of(PricingPolicy p) {
      return new PricingResponse(
          p.gracePeriod().toMinutes(),
          p.firstHourCents(),
          p.fraction().toMinutes(),
          p.fractionCents(),
          p.dailyCapCents());
    }
  }

  public record LotResponse(UUID id, String name, PricingResponse pricing, Instant createdAt) {
    static LotResponse of(ParkingLot lot) {
      return new LotResponse(
          lot.getId(), lot.getName(), PricingResponse.of(lot.pricingPolicy()), lot.getCreatedAt());
    }
  }

  public record QuoteResponse(Instant entryAt, Instant exitAt, long amountCents) {}

  public record TypeAvailability(
      SpotType type, long total, long occupied, long reserved, long available) {}

  public record AvailabilityResponse(
      UUID lotId,
      Instant at,
      long total,
      long occupied,
      long reserved,
      long available,
      List<TypeAvailability> byType) {}
}
