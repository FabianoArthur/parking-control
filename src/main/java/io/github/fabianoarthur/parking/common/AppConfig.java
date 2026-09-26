package io.github.fabianoarthur.parking.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class AppConfig {

  /**
   * Every "now" in the domain comes from this clock, so tests can control time. Millisecond ticks
   * match what the database stores, so an instant reads back exactly as it was written.
   */
  @Bean
  Clock clock() {
    return Clock.tickMillis(ZoneOffset.UTC);
  }

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Parking Control API")
                .version("v1")
                .description(
                    "Parking lots, spots, reservations, check-in/check-out and time-based"
                        + " pricing. Amounts are integer cents. Timestamps are ISO-8601 with an"
                        + " offset (e.g. 2026-03-02T08:00:00Z).")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
  }
}
