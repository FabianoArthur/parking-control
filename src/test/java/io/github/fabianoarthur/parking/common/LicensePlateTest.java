package io.github.fabianoarthur.parking.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LicensePlateTest {

  @ParameterizedTest
  @CsvSource({
    "abc1234, ABC1234", // pre-2018 format
    "ABC-1234, ABC1234",
    "abc1d23, ABC1D23", // Mercosul format
    "' ABC 1D23 ', ABC1D23",
  })
  void normalizesValidPlates(String raw, String expected) {
    assertThat(LicensePlate.of(raw).value()).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "AB1234", "ABCD123", "1234ABC", "ABC12345", "ABC1DD3"})
  void rejectsInvalidPlates(String raw) {
    assertThatThrownBy(() -> LicensePlate.of(raw)).isInstanceOf(BusinessRuleException.class);
  }
}
