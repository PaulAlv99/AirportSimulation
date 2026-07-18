package edu.uni.airportsim.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimulationApiIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("airport_simulation")
            .withUsername("airport")
            .withPassword("airport");

    private static final Path DATA_DIR = createDataDir();

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeAll
    static void createImportFiles() {
        Path importDir = DATA_DIR.resolve("import");
        try {
            Files.createDirectories(importDir);
            write(importDir.resolve("countries.csv"), """
                    row_id,code,name,continent
                    1,PT,Portugal,EU
                    """);
            write(importDir.resolve("regions.csv"), """
                    row_id,code,local_code,name,continent,iso_country
                    1,PT-11,11,Lisbon,EU,PT
                    """);
            write(importDir.resolve("airports.csv"), """
                    id,ident,type,name,latitude_deg,longitude_deg,elevation_ft,continent,iso_country,iso_region,municipality,scheduled_service,gps_code,iata_code,local_code,extra_1,extra_2,extra_3
                    1,TEST,small_airport,Test Airport,38.7223,-9.1393,114,EU,PT,PT-11,Lisbon,yes,TEST,TST,TST,,,
                    2,PORTO,medium_airport,Porto Airport,41.2481,-8.6814,228,EU,PT,PT-13,Porto,yes,PORTO,OPO,OPO,,,
                    3,FUNCHAL,medium_airport,Madeira Airport,32.6979,-16.7745,192,EU,PT,PT-30,Funchal,yes,FUNCHAL,FNC,FNC,,,
                    """);
            write(importDir.resolve("runways.csv"), """
                    id,airport_ref,airport_ident,length_ft,width_ft,surface,lighted,closed,le_ident
                    1,1,TST,3000,45,ASPH,1,0,18
                    2,2,PORTO,11417,148,ASPH,1,0,17
                    """);
            write(importDir.resolve("navaids.csv"), """
                    id,filename,ident,name,type,frequency_khz,latitude_deg,longitude_deg,elevation_ft,iso_country,magnetic_variation_deg,usageType,power,associated_airport
                    1,Test_Navaid,TS1,Test NDB,NDB,345,38.7,-9.1,10,PT,0,LO,MEDIUM,TST
                    """);
            write(importDir.resolve("airlines_flights_data.csv"), """
                    row_id,airline,flight,source_city,departure_time,stops,arrival_time,destination_city,class,duration,days_left,price
                    1,TAP Air Portugal,TP100,Lisbon,Evening,zero,Night,Porto,Economy,1.0,1,85
                    2,TAP Air Portugal,TP200,Porto,Evening,zero,Night,Lisbon,Economy,1.0,1,90
                    3,Azores Airlines,S4123,Lisbon,Night,zero,Morning,Funchal,Business,2.5,2,210
                    """);
            Path openFlightsDir = importDir.resolve("openflights");
            Files.createDirectories(openFlightsDir);
            write(openFlightsDir.resolve("airlines.dat"), """
                    123,"TAP Air Portugal",\\N,"TP","TAP","AIR PORTUGAL","Portugal","Y"
                    456,"Azores Airlines",\\N,"S4","RZO","AIR AZORES","Portugal","Y"
                    """);
            write(openFlightsDir.resolve("planes.dat"), """
                    "Airbus A320","320","A320"
                    "ATR 72","AT7","AT72"
                    """);
            write(openFlightsDir.resolve("routes.dat"), """
                    TP,123,TST,1001,OPO,1002,,0,320
                    TP,123,OPO,1002,TST,1001,,0,320
                    S4,456,TST,1001,FNC,1003,,0,AT7
                    S4,456,FNC,1003,TST,1001,,0,AT7
                    """);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create test import files", exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("airport-simulation.data-dir", () -> DATA_DIR.toString());
        registry.add("airport-simulation.default-airport-code", () -> "TST");
        registry.add("airport-simulation.flight-seed-limit", () -> 3);
        registry.add("airport-simulation.demo-flight-count", () -> 3);
        registry.add("airport-simulation.target-daily-flights", () -> 6);
        registry.add("airport-simulation.passenger-load-factor", () -> 0.8);
        registry.add("airport-simulation.bag-rate", () -> 0.7);
        registry.add("airport-simulation.use-open-flights", () -> true);
        registry.add("airport-simulation.random-seed", () -> 12345L);
        registry.add("airport-simulation.operating-start-time", () -> "05:00");
        registry.add("airport-simulation.operating-end-time", () -> "23:00");
        registry.add("airport-simulation.planning-horizon-minutes", () -> 360);
        registry.add("airport-simulation.target-daily-flights", () -> 60);
        registry.add("airport-simulation.delay-probability", () -> 0.75);
        registry.add("airport-simulation.baggage-exception-probability", () -> 0.5);
        registry.add("airport-simulation.passenger-no-show-probability", () -> 0.2);
        registry.add("airport-simulation.ground-jitter-minutes", () -> 20);
        registry.add("airport-simulation.tick-interval", () -> "1h");
        registry.add("server.servlet.context-path", () -> "/airport-simulation");
        registry.add("debug", () -> "false");
    }

    @Test
    void bootsAtNestedPathAndRespondsToControls() throws Exception {
        JsonNode snapshot = snapshot();
        assertThat(snapshot.path("running").asBoolean()).isTrue();
        assertThat(snapshot.path("airport").path("code").asText()).isEqualTo("TST");
        assertThat(snapshot.path("counts").path("airports").asLong()).isEqualTo(3L);
        assertThat(snapshot.path("counts").path("openFlightRoutes").asLong()).isEqualTo(4L);
        assertThat(snapshot.path("counts").path("demoFlights").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("counts").path("baggage").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("flights")).isNotEmpty();
        assertThat(snapshot.path("settings").path("targetDailyFlights").asInt()).isEqualTo(60);
        assertThat(snapshot.path("generation").path("open").asBoolean()).isTrue();
        assertThat(snapshot.path("generation").path("runSeed").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("generation").path("generatedFlights").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("operations").path("passengersTotal").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("operations").path("baggageTotal").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("operations").path("gatesTotal").asLong()).isGreaterThan(0L);
        assertThat(snapshot.path("operations").path("stochasticMode").asBoolean()).isTrue();
        assertThat(snapshot.path("operations").path("randomSeed").asLong()).isEqualTo(12345L);
        assertThat(snapshot.path("operations").path("delayProbability").asDouble()).isEqualTo(0.75);

        ResponseEntity<String> airports = restTemplate.getForEntity(baseUrl() + "/api/airports?q=porto&limit=10", String.class);
        assertThat(airports.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode airportResults = objectMapper.readTree(airports.getBody());
        assertThat(airportResults).hasSize(1);
        assertThat(airportResults.get(0).path("code").asText()).isEqualTo("OPO");

        post("/api/airport/select", """
                {"code":"OPO"}
                """);
        JsonNode portoSnapshot = snapshot();
        assertThat(portoSnapshot.path("airport").path("code").asText()).isEqualTo("OPO");
        assertThat(portoSnapshot.path("airport").path("runways").asLong()).isEqualTo(1L);
        assertThat(portoSnapshot.path("flights").get(0).path("originLabel").asText()
                + portoSnapshot.path("flights").get(0).path("destinationLabel").asText()).contains("OPO");

        post("/api/weather/manual", """
                {
                  "temperatureCelsius": 8.5,
                  "feelsLikeCelsius": 5.5,
                  "windSpeedKmh": 22.0,
                  "windGustKmh": 34.0,
                  "windDirectionDegrees": 250,
                  "rainMmPerHour": 1.2,
                  "snowMmPerHour": 0.0,
                  "hail": false,
                  "thunderstorm": false,
                  "visibilityMeters": 4600,
                  "fog": true,
                  "cloudCoveragePercent": 88,
                  "ceilingMeters": 700,
                  "cloudLabel": "Fog",
                  "runwaySurface": "WET",
                  "severityCode": "CAUTION"
                }
                """);
        JsonNode weatherSnapshot = snapshot();
        assertThat(weatherSnapshot.path("weather").path("airportId").asText()).isEqualTo("OPO");
        assertThat(weatherSnapshot.path("weather").path("temperatureCelsius").asDouble()).isEqualTo(8.5);
        assertThat(weatherSnapshot.path("weather").path("runwaySurface").asText()).isEqualTo("WET");

        post("/api/control/pause", null);
        assertThat(snapshot().path("running").asBoolean()).isFalse();

        post("/api/control/multiplier", """
                {"multiplier":"x10"}
                """);
        assertThat(snapshot().path("multiplier").asText()).isEqualTo("x10");

        post("/api/control/start", null);
        assertThat(snapshot().path("running").asBoolean()).isTrue();
    }

    @Test
    void seededProbabilitiesCreateBoundedRandomizedOperations() throws Exception {
        JsonNode snapshot = snapshot();
        Set<Integer> passengerCounts = new HashSet<>();
        for (JsonNode flight : snapshot.path("flights")) {
            passengerCounts.add(flight.path("passengerCount").asInt());
        }
        assertThat(passengerCounts.size()).isGreaterThan(1);

        JsonNode operations = snapshot.path("operations");
        assertThat(operations.path("bagsDelayed").asLong() + operations.path("bagsLost").asLong()).isGreaterThan(0L);
        assertThat(operations.path("passengersMissedConnections").asLong()).isGreaterThan(0L);
        assertThat(operations.path("groundJitterMinutes").asInt()).isEqualTo(20);
    }

    @Test
    void exposesOperationsDashboardsAndControls() throws Exception {
        try {
            JsonNode summary = getJson("/api/operations/summary");
            assertThat(summary.path("passengersTotal").asLong()).isGreaterThan(0L);
            assertThat(summary.path("baggageTotal").asLong()).isGreaterThan(0L);
            assertThat(summary.path("checkInQueue").asLong()).isGreaterThanOrEqualTo(0L);

            JsonNode settings = getJson("/api/settings");
            assertThat(settings.path("operatingStartTime").asText()).startsWith("05:00");
            assertThat(settings.path("retentionDays").asInt()).isEqualTo(7);

            put("/api/settings", """
                    {
                      "operatingStartTime": "05:00",
                      "operatingEndTime": "23:00",
                      "trafficProfile": "BUSY",
                      "targetDailyFlights": 72,
                      "passengerLoadFactor": 0.88,
                      "bagRate": 0.75,
                      "staffingMultiplier": 1.4,
                      "delayProbability": 0.6,
                      "baggageExceptionProbability": 0.4,
                      "passengerNoShowProbability": 0.15,
                      "groundJitterMinutes": 18,
                      "planningHorizonMinutes": 300,
                      "retentionDays": 9,
                      "randomSeed": 9999
                    }
                    """);
            JsonNode updatedSettings = getJson("/api/settings");
            assertThat(updatedSettings.path("targetDailyFlights").asInt()).isEqualTo(72);
            assertThat(updatedSettings.path("staffingMultiplier").asDouble()).isEqualTo(1.4);
            assertThat(updatedSettings.path("retentionDays").asInt()).isEqualTo(9);

            JsonNode bags = getJson("/api/operations/baggage?limit=2");
            assertThat(bags).hasSizeLessThanOrEqualTo(2);
            assertThat(bags.get(0).path("tag").asText()).startsWith("BAG");

            JsonNode screenedBags = getJson("/api/operations/baggage?status=SCREENED&limit=10");
            for (JsonNode bag : screenedBags) {
                assertThat(bag.path("status").asText()).isEqualTo("SCREENED");
            }

            JsonNode gates = getJson("/api/operations/gates");
            assertThat(gates).isNotEmpty();
            assertThat(gates.get(0).path("gateCode").asText()).isNotBlank();

            long manifestFlightId = snapshot().path("flights").get(0).path("id").asLong();
            JsonNode passengers = getJson("/api/flights/" + manifestFlightId + "/passengers?limit=5");
            assertThat(passengers).isNotEmpty();
            assertThat(passengers.get(0).path("fullName").asText()).contains(" ");
            assertThat(passengers.get(0).path("seatNumber").asText()).isNotBlank();

            JsonNode ground = getJson("/api/operations/ground");
            assertThat(ground).isNotEmpty();
            assertThat(ground.get(0).path("operationType").asText()).isNotBlank();

            long flightId = snapshot().path("flights").get(0).path("id").asLong();
            post("/api/flights/" + flightId + "/control", """
                    {"status":"DELAYED","delayMinutes":25,"reason":"integration test delay"}
                    """);
            JsonNode delayedFlight = findFlight(snapshot(), flightId);
            assertThat(delayedFlight.path("status").asText()).isEqualTo("DELAYED");
            assertThat(delayedFlight.path("delayMinutes").asInt()).isEqualTo(25);
            assertThat(delayedFlight.path("delayReason").asText()).contains("integration test delay");

            post("/api/operations/disruption", """
                    {"type":"BAGGAGE_JAM","severity":2,"durationMinutes":20}
                    """);
            JsonNode disrupted = getJson("/api/operations/summary");
            assertThat(disrupted.path("baggageBacklog").asLong()).isGreaterThan(0L);
        } finally {
            put("/api/settings", """
                    {
                      "operatingStartTime": "05:00",
                      "operatingEndTime": "23:00",
                      "trafficProfile": "BUSY",
                      "targetDailyFlights": 60,
                      "passengerLoadFactor": 0.8,
                      "bagRate": 0.7,
                      "staffingMultiplier": 1.0,
                      "delayProbability": 0.75,
                      "baggageExceptionProbability": 0.5,
                      "passengerNoShowProbability": 0.2,
                      "groundJitterMinutes": 20,
                      "planningHorizonMinutes": 360,
                      "retentionDays": 7,
                      "randomSeed": 12345
                    }
                    """);
        }
    }

    @Test
    void resetClampsToNextOpeningWhenOutsideOperatingHours() throws Exception {
        put("/api/settings", """
                {
                  "operatingStartTime": "11:00",
                  "operatingEndTime": "12:00",
                  "trafficProfile": "CALM",
                  "targetDailyFlights": 24,
                  "passengerLoadFactor": 0.75,
                  "bagRate": 0.65,
                  "staffingMultiplier": 1.1,
                  "delayProbability": 0.2,
                  "baggageExceptionProbability": 0.05,
                  "passengerNoShowProbability": 0.04,
                  "groundJitterMinutes": 10,
                  "planningHorizonMinutes": 120,
                  "retentionDays": 7,
                  "randomSeed": 321
                }
                """);
        post("/api/control/reset", null);
        JsonNode snapshot = snapshot();
        assertThat(snapshot.path("generation").path("open").asBoolean()).isTrue();
        assertThat(snapshot.path("simulatedTime").asText()).startsWith("2026-01-01T11:00");
        assertThat(snapshot.path("generation").path("nextOpeningAt").asText()).startsWith("2026-01-02T11:00");
        assertThat(snapshot.path("flights")).isNotEmpty();
    }

    private JsonNode snapshot() throws Exception {
        return getJson("/api/snapshot");
    }

    private JsonNode getJson(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + path, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private void post(String path, String body) {
        ResponseEntity<String> response;
        if (body == null) {
            response = restTemplate.postForEntity(baseUrl() + path, null, String.class);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            response = restTemplate.postForEntity(baseUrl() + path, new HttpEntity<>(body, headers), String.class);
        }
        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 204).isTrue();
    }

    private void put(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + path,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    private String baseUrl() {
        return "http://localhost:" + port + "/airport-simulation";
    }

    private JsonNode findFlight(JsonNode snapshot, long flightId) {
        for (JsonNode flight : snapshot.path("flights")) {
            if (flight.path("id").asLong() == flightId) {
                return flight;
            }
        }
        throw new AssertionError("Flight not found: " + flightId);
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("airport-sim-it");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content.stripIndent().stripTrailing() + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}
