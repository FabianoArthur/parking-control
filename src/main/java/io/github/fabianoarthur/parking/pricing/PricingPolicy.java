package io.github.fabianoarthur.parking.pricing;

import java.time.Duration;

/**
 * How a lot charges for a stay. All prices are integer cents.
 *
 * @param gracePeriod stays up to this long are free
 * @param firstHourCents price of the first hour (or any part of it)
 * @param fraction billing unit after the first hour
 * @param fractionCents price of each started fraction after the first hour
 * @param dailyCapCents maximum charged for any 24-hour block
 */
public record PricingPolicy(
    Duration gracePeriod,
    long firstHourCents,
    Duration fraction,
    long fractionCents,
    long dailyCapCents) {

  public PricingPolicy {
    if (gracePeriod == null || gracePeriod.isNegative()) {
      throw new IllegalArgumentException("gracePeriod must be zero or positive");
    }
    if (fraction == null || fraction.isZero() || fraction.isNegative()) {
      throw new IllegalArgumentException("fraction must be positive");
    }
    if (firstHourCents < 0 || fractionCents < 0 || dailyCapCents < 0) {
      throw new IllegalArgumentException("prices must be zero or positive");
    }
    if (dailyCapCents < firstHourCents) {
      throw new IllegalArgumentException("dailyCapCents must be at least firstHourCents");
    }
  }
}
