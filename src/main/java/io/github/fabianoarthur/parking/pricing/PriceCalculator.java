package io.github.fabianoarthur.parking.pricing;

import java.time.Duration;
import java.time.Instant;

/**
 * Prices a stay from elapsed time only, so time zones and daylight-saving changes never affect the
 * amount. A "day" is 24 hours of elapsed time.
 */
public final class PriceCalculator {

  private static final Duration DAY = Duration.ofHours(24);
  private static final Duration HOUR = Duration.ofHours(1);

  private PriceCalculator() {}

  public static long priceCents(PricingPolicy policy, Instant entryAt, Instant exitAt) {
    if (exitAt.isBefore(entryAt)) {
      throw new IllegalArgumentException("exit must not be before entry");
    }
    Duration stay = Duration.between(entryAt, exitAt);
    if (stay.compareTo(policy.gracePeriod()) <= 0) {
      return 0;
    }
    long fullDays = stay.dividedBy(DAY);
    Duration remainder = stay.minus(DAY.multipliedBy(fullDays));
    return Math.addExact(
        Math.multiplyExact(fullDays, policy.dailyCapCents()), partialDayCents(policy, remainder));
  }

  private static long partialDayCents(PricingPolicy policy, Duration remainder) {
    if (remainder.isZero()) {
      return 0;
    }
    long cents = policy.firstHourCents();
    Duration afterFirstHour = remainder.minus(HOUR);
    if (afterFirstHour.isPositive()) {
      cents += startedFractions(afterFirstHour, policy.fraction()) * policy.fractionCents();
    }
    return Math.min(cents, policy.dailyCapCents());
  }

  private static long startedFractions(Duration duration, Duration fraction) {
    long whole = duration.dividedBy(fraction);
    return duration.equals(fraction.multipliedBy(whole)) ? whole : whole + 1;
  }
}
