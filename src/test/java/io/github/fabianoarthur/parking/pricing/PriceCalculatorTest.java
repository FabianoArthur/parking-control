package io.github.fabianoarthur.parking.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceCalculatorTest {

  // 15 min free, R$ 10.00 first hour, R$ 3.00 per started 30 min, R$ 60.00 daily cap
  private static final PricingPolicy POLICY =
      new PricingPolicy(Duration.ofMinutes(15), 1000, Duration.ofMinutes(30), 300, 6000);

  private static final Instant ENTRY = Instant.parse("2026-03-01T08:00:00Z");

  @ParameterizedTest(name = "{0} min -> {1} cents")
  @CsvSource({
    "0, 0",
    "15, 0", // grace period is inclusive
    "16, 1000", // past grace: first hour is charged in full
    "60, 1000",
    "61, 1300", // one started fraction
    "90, 1300",
    "91, 1600",
    "180, 2200", // 1h + 4 fractions
    "600, 6000", // 1h + 18 fractions = 64.00, capped at 60.00
    "1440, 6000", // exactly one day
    "1441, 7000", // one day + first hour of the next
    "1455, 7000", // grace does not apply to the remainder of a multi-day stay
    "2880, 12000", // two full days
    "4320, 18000", // three full days
  })
  void chargesByStartedFractionWithDailyCap(long minutes, long expectedCents) {
    Instant exit = ENTRY.plus(Duration.ofMinutes(minutes));

    assertThat(PriceCalculator.priceCents(POLICY, ENTRY, exit)).isEqualTo(expectedCents);
  }

  @Test
  void secondsCountAsAStartedFraction() {
    Instant exit = ENTRY.plus(Duration.ofMinutes(60).plusSeconds(1));

    assertThat(PriceCalculator.priceCents(POLICY, ENTRY, exit)).isEqualTo(1300);
  }

  @Test
  void zeroGraceChargesFirstHourForAnyStay() {
    var noGrace = new PricingPolicy(Duration.ZERO, 800, Duration.ofMinutes(15), 200, 5000);

    assertThat(PriceCalculator.priceCents(noGrace, ENTRY, ENTRY)).isZero();
    assertThat(PriceCalculator.priceCents(noGrace, ENTRY, ENTRY.plusSeconds(1))).isEqualTo(800);
  }

  @Test
  void rejectsExitBeforeEntry() {
    assertThatThrownBy(() -> PriceCalculator.priceCents(POLICY, ENTRY, ENTRY.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exit");
  }

  @Test
  void rejectsInvalidPolicy() {
    assertThatThrownBy(
            () -> new PricingPolicy(Duration.ofMinutes(5), 100, Duration.ZERO, 100, 1000))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new PricingPolicy(Duration.ofMinutes(5), -1, Duration.ofMinutes(30), 100, 1000))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> new PricingPolicy(Duration.ofMinutes(5), 1000, Duration.ofMinutes(30), 100, 999))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dailyCap");
  }

  @Test
  void daylightSavingChangeDoesNotChangeThePrice() {
    // New York springs forward on 2026-03-08: 01:30 EST -> 03:30 EDT is one hour of real time
    Instant entry =
        java.time.ZonedDateTime.parse("2026-03-08T01:30-05:00[America/New_York]").toInstant();
    Instant exit =
        java.time.ZonedDateTime.parse("2026-03-08T03:30-04:00[America/New_York]").toInstant();

    assertThat(PriceCalculator.priceCents(POLICY, entry, exit)).isEqualTo(1000);
  }
}
