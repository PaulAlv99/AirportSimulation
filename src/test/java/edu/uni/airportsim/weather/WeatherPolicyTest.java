package edu.uni.airportsim.weather;

import edu.uni.airportsim.domain.Address;
import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Runway;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherPolicyTest {
    @Test
    void calculatesCompassDirectionAndCrosswind() {
        Wind wind = new Wind(40, 55, 270);

        assertEquals("W", wind.compassDirection());
        assertTrue(wind.crosswindKmh(180) > 39);
    }

    @Test
    void createsSevereAlertsForStormsAndContaminatedRunways() {
        Airport airport = new Airport("APT-LIS", "Lisbon", "LIS", new Address("Lisbon", "Portugal"));
        airport.addRunway(new Runway("RWY-1", "Runway 18", 180));
        WeatherSnapshot snapshot = new WeatherSnapshot(
                "APT-LIS",
                LocalDateTime.of(2026, 1, 1, 8, 0),
                new Temperature(4, 0),
                new Wind(50, 75, 270),
                new Precipitation(0, 2, false, true),
                new Visibility(700, true),
                new CloudCondition(100, 300, "Storm"),
                RunwaySurfaceCondition.SNOW,
                WeatherSeverity.NORMAL
        );

        List<WeatherAlert> alerts = new DefaultWeatherImpactPolicy().evaluate(airport, snapshot);

        assertTrue(alerts.stream().anyMatch(alert -> alert.severity().code().equals("GROUND_STOP")));
        assertTrue(alerts.stream().anyMatch(alert -> alert.runwayId().equals("RWY-1")));
    }
}
