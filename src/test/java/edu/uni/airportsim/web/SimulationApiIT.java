package edu.uni.airportsim.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        registry.add("airport-simulation.flight-seed-limit", () -> 3);
        registry.add("airport-simulation.demo-flight-count", () -> 3);
        registry.add("airport-simulation.tick-interval", () -> "1h");
        registry.add("server.servlet.context-path", () -> "/projects/airport-simulation");
    }

    @Test
    void bootsAtNestedPathAndRespondsToControls() throws Exception {
        JsonNode snapshot = snapshot();
        assertThat(snapshot.path("running").asBoolean()).isTrue();
        assertThat(snapshot.path("airport").path("code").asText()).isEqualTo("TST");
        assertThat(snapshot.path("counts").path("airports").asLong()).isEqualTo(2L);
        assertThat(snapshot.path("counts").path("demoFlights").asLong()).isEqualTo(3L);
        assertThat(snapshot.path("flights")).hasSize(3);

        ResponseEntity<String> airports = restTemplate.getForEntity(baseUrl() + "/api/airports", String.class);
        assertThat(airports.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(objectMapper.readTree(airports.getBody())).hasSize(2);

        post("/api/airport/select", """
                {"code":"OPO"}
                """);
        JsonNode portoSnapshot = snapshot();
        assertThat(portoSnapshot.path("airport").path("code").asText()).isEqualTo("OPO");
        assertThat(portoSnapshot.path("airport").path("runways").asLong()).isEqualTo(1L);

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

    private JsonNode snapshot() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/api/snapshot", String.class);
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

    private String baseUrl() {
        return "http://localhost:" + port + "/projects/airport-simulation";
    }

    private static Path createDataDir() {
        try {
            return Files.createTempDirectory("airport-sim-it");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(file, content.stripIndent().stripTrailing() + System.lineSeparator(), StandardCharsets.UTF_8);
    }
}
