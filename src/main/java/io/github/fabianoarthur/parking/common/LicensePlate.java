package io.github.fabianoarthur.parking.common;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A Brazilian license plate, normalized to upper case without separators. Accepts the pre-2018
 * format ({@code ABC1234}) and the Mercosul format ({@code ABC1D23}).
 */
public record LicensePlate(String value) {

  private static final Pattern VALID = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

  public static LicensePlate of(String raw) {
    String normalized = raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    if (!VALID.matcher(normalized).matches()) {
      throw new BusinessRuleException(
          "INVALID_LICENSE_PLATE", "'" + raw + "' is not a valid license plate");
    }
    return new LicensePlate(normalized);
  }

  @Override
  public String toString() {
    return value;
  }
}
