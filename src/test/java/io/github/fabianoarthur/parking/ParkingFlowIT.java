package io.github.fabianoarthur.parking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingFlowIT {

  private static final Instant T0 = Instant.parse("2026-03-02T08:00:00Z");

  @TestConfiguration
  static class ClockConfig {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(T0);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired MutableClock clock;
  @Autowired JdbcTemplate jdbc;

  private String lotId;

  @BeforeEach
  void createLotWithSpots() throws Exception {
    jdbc.execute("DELETE FROM parking_session");
    jdbc.execute("DELETE FROM reservation");
    jdbc.execute("DELETE FROM parking_spot");
    jdbc.execute("DELETE FROM parking_lot");
    clock.set(T0);
    lotId =
        idOf(
            postJson(
                    "/api/v1/lots",
                    """
                    {"name": "Downtown Garage",
                     "pricing": {"gracePeriodMinutes": 15, "firstHourCents": 1000,
                                 "fractionMinutes": 30, "fractionCents": 300,
                                 "dailyCapCents": 6000}}
                    """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/lots/"))));
    postJson(spots(), "{\"code\": \"A-01\", \"type\": \"STANDARD\"}")
        .andExpect(status().isCreated());
    postJson(spots(), "{\"code\": \"A-02\", \"type\": \"STANDARD\"}")
        .andExpect(status().isCreated());
    postJson(spots(), "{\"code\": \"E-01\", \"type\": \"EV\"}").andExpect(status().isCreated());
  }

  @Test
  void walkInStayIsChargedOnCheckout() throws Exception {
    String sessionId =
        idOf(
            postJson(sessions(), "{\"licensePlate\": \"abc-1d23\", \"spotType\": \"STANDARD\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spotCode").value("A-01"))
                .andExpect(jsonPath("$.licensePlate").value("ABC1D23"))
                .andExpect(jsonPath("$.status").value("OPEN")));

    clock.advance(Duration.ofMinutes(95));

    mvc.perform(post("/api/v1/sessions/{id}/checkout", sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"))
        .andExpect(jsonPath("$.amountCents").value(1600))
        .andExpect(jsonPath("$.exitAt").value("2026-03-02T09:35:00Z"));

    mvc.perform(post("/api/v1/sessions/{id}/checkout", sessionId)).andExpect(status().isConflict());
  }

  @Test
  void availabilityReflectsOccupiedAndReservedSpots() throws Exception {
    postJson(sessions(), "{\"licensePlate\": \"ABC1234\", \"spotCode\": \"A-01\"}")
        .andExpect(status().isCreated());
    postJson(
            reservations(),
            reservation("E-01", "XYZ9A87", T0.minusSeconds(60), T0.plus(Duration.ofHours(1))))
        .andExpect(status().isUnprocessableEntity()); // cannot start in the past
    postJson(
            reservations(),
            reservation("E-01", "XYZ9A87", T0.plusSeconds(60), T0.plus(Duration.ofHours(2))))
        .andExpect(status().isCreated());
    clock.advance(Duration.ofMinutes(5));

    mvc.perform(get("/api/v1/lots/{id}/availability", lotId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.occupied").value(1))
        .andExpect(jsonPath("$.reserved").value(1))
        .andExpect(jsonPath("$.available").value(1))
        .andExpect(jsonPath("$.byType[?(@.type == 'EV')].available").value(hasItem(0)));
  }

  @Test
  void reservedSpotIsHeldForItsPlate() throws Exception {
    String reservationId =
        idOf(
            postJson(
                    reservations(),
                    reservation(
                        "E-01",
                        "XYZ9A87",
                        T0.plus(Duration.ofMinutes(30)),
                        T0.plus(Duration.ofHours(3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE")));

    // overlapping reservation on the same spot is rejected
    postJson(
            reservations(),
            reservation(
                "E-01", "DEF4567", T0.plus(Duration.ofHours(2)), T0.plus(Duration.ofHours(4))))
        .andExpect(status().isConflict());

    clock.advance(Duration.ofMinutes(40));

    // another car cannot take the reserved spot while the reservation is in effect
    postJson(sessions(), "{\"licensePlate\": \"DEF4567\", \"spotCode\": \"E-01\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SPOT_RESERVED"));

    // the reservation holder is sent to the reserved spot
    postJson(sessions(), "{\"licensePlate\": \"XYZ9A87\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.spotCode").value("E-01"))
        .andExpect(jsonPath("$.reservationId").value(reservationId));

    mvc.perform(get("/api/v1/reservations/{id}", reservationId))
        .andExpect(jsonPath("$.status").value("FULFILLED"));
    mvc.perform(delete("/api/v1/reservations/{id}", reservationId))
        .andExpect(status().isConflict());
  }

  @Test
  void futureReservationCanBeCancelled() throws Exception {
    String reservationId =
        idOf(
            postJson(
                reservations(),
                reservation(
                    "A-02",
                    "XYZ9A87",
                    T0.plus(Duration.ofHours(1)),
                    T0.plus(Duration.ofHours(2)))));

    mvc.perform(delete("/api/v1/reservations/{id}", reservationId))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/reservations/{id}", reservationId))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void samePlateCannotBeParkedTwice() throws Exception {
    postJson(sessions(), "{\"licensePlate\": \"ABC1234\"}").andExpect(status().isCreated());

    postJson(sessions(), "{\"licensePlate\": \"abc-1234\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PLATE_ALREADY_PARKED"));
  }

  @Test
  void occupiedSpotAndFullLotAreConflicts() throws Exception {
    postJson(sessions(), "{\"licensePlate\": \"ABC1234\", \"spotCode\": \"E-01\"}")
        .andExpect(status().isCreated());

    postJson(sessions(), "{\"licensePlate\": \"DEF4567\", \"spotCode\": \"E-01\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SPOT_OCCUPIED"));
    postJson(sessions(), "{\"licensePlate\": \"DEF4567\", \"spotType\": \"EV\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NO_SPOT_AVAILABLE"));
  }

  @Test
  void quoteUsesTheLotPricing() throws Exception {
    mvc.perform(
            get("/api/v1/lots/{id}/quote", lotId)
                .param("entryAt", "2026-03-02T08:00:00Z")
                .param("exitAt", "2026-03-03T09:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amountCents").value(7000));

    mvc.perform(
            get("/api/v1/lots/{id}/quote", lotId)
                .param("entryAt", "2026-03-02T10:00:00Z")
                .param("exitAt", "2026-03-02T08:00:00Z"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void errorsAreProblemDetails() throws Exception {
    postJson("/api/v1/lots", "{\"name\": \"\", \"pricing\": {\"fractionMinutes\": 0}}")
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Content-Type", "application/problem+json"))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("name")))
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("pricing.fractionMinutes")));

    mvc.perform(get("/api/v1/lots/{id}", "5b0c7a4e-1f3a-4f8e-9d59-2f1c7e0b9a11"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    mvc.perform(get("/api/v1/lots/{id}", "not-a-uuid")).andExpect(status().isBadRequest());

    postJson(
            "/api/v1/lots",
            """
            {"name": "Too Expensive",
             "pricing": {"gracePeriodMinutes": 0, "firstHourCents": 1,
                         "fractionMinutes": 1, "fractionCents": 1,
                         "dailyCapCents": 9223372036854775807}}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field").value(hasItem("pricing.dailyCapCents")));

    postJson(sessions(), "{\"licensePlate\": \"NOPE\"}")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_LICENSE_PLATE"));

    postJson(spots(), "{\"code\": \"A-01\", \"type\": \"STANDARD\"}")
        .andExpect(status().isConflict());

    postJson(
            reservations(),
            "{\"spotCode\": \"A-02\", \"licensePlate\": \"ABC1234\","
                + " \"startsAt\": \"2026-03-02T09:00:00\", \"endsAt\": \"2026-03-02T10:00:00Z\"}")
        .andExpect(status().isBadRequest()); // timestamp without offset
  }

  @Test
  void listsArePaged() throws Exception {
    mvc.perform(get("/api/v1/lots/{id}/spots", lotId).param("type", "STANDARD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.page.totalElements").value(2));
    mvc.perform(get("/api/v1/lots").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)));
  }

  @Test
  void racingCheckInsNeverDoubleBook() throws Exception {
    // same car at two gates, and two cars racing for the same spot
    List<Integer> samePlate =
        race(
            () -> postJson(sessions(), "{\"licensePlate\": \"RAC1E01\", \"spotCode\": \"A-01\"}"),
            () -> postJson(sessions(), "{\"licensePlate\": \"RAC1E01\", \"spotCode\": \"A-02\"}"));
    List<Integer> sameSpot =
        race(
            () -> postJson(sessions(), "{\"licensePlate\": \"RAC2E02\", \"spotCode\": \"E-01\"}"),
            () -> postJson(sessions(), "{\"licensePlate\": \"RAC3E03\", \"spotCode\": \"E-01\"}"));

    assertThat(samePlate).containsExactlyInAnyOrder(201, 409);
    assertThat(sameSpot).containsExactlyInAnyOrder(201, 409);
  }

  private static List<Integer> race(Callable<ResultActions> first, Callable<ResultActions> second)
      throws Exception {
    try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Integer>> results = new ArrayList<>();
      for (Callable<ResultActions> call : List.of(first, second)) {
        results.add(
            pool.submit(
                () -> {
                  start.await();
                  return call.call().andReturn().getResponse().getStatus();
                }));
      }
      start.countDown();
      List<Integer> statuses = new ArrayList<>();
      for (Future<Integer> result : results) {
        statuses.add(result.get());
      }
      return statuses;
    }
  }

  private String spots() {
    return "/api/v1/lots/" + lotId + "/spots";
  }

  private String sessions() {
    return "/api/v1/lots/" + lotId + "/sessions";
  }

  private String reservations() {
    return "/api/v1/lots/" + lotId + "/reservations";
  }

  private static String reservation(String spot, String plate, Instant start, Instant end) {
    return """
        {"spotCode": "%s", "licensePlate": "%s", "startsAt": "%s", "endsAt": "%s"}
        """
        .formatted(spot, plate, start, end);
  }

  private ResultActions postJson(String url, String body) throws Exception {
    return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private static String idOf(ResultActions result) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
  }
}
