package edu.uni.airportsim.weather;

import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.domain.Runway;

import java.util.ArrayList;
import java.util.List;

public class DefaultWeatherImpactPolicy implements WeatherImpactPolicy {
    @Override
    public List<WeatherAlert> evaluate(Airport airport, WeatherSnapshot snapshot) {
        if (airport == null || snapshot == null) {
            return List.of();
        }

        List<WeatherAlert> alerts = new ArrayList<>();
        WeatherSeverity severity = WeatherSeverity.NORMAL;

        if (snapshot.precipitation().thunderstorm()) {
            severity = severity.max(WeatherSeverity.GROUND_STOP);
            alerts.add(alert(snapshot, WeatherSeverity.GROUND_STOP, "Thunderstorm requires ground operation stop", airport.getId(), "", ""));
        }
        if (snapshot.visibility().isLowVisibility()) {
            severity = severity.max(WeatherSeverity.SEVERE);
            alerts.add(alert(snapshot, WeatherSeverity.SEVERE, "Low visibility or fog affects arrivals and departures", airport.getId(), "", ""));
        }
        if (snapshot.precipitation().rainMmPerHour() >= 8) {
            severity = severity.max(WeatherSeverity.CAUTION);
            alerts.add(alert(snapshot, WeatherSeverity.CAUTION, "Heavy rain reduces runway braking performance", airport.getId(), "", ""));
        }
        if (snapshot.precipitation().snowMmPerHour() > 0 || snapshot.runwaySurfaceCondition().isContaminated()) {
            severity = severity.max(WeatherSeverity.SEVERE);
            alerts.add(alert(snapshot, WeatherSeverity.SEVERE, "Snow, ice, or contaminated runway surface requires runway checks", airport.getId(), "", ""));
        }
        for (Runway runway : airport.getRunways()) {
            double crosswind = snapshot.wind().crosswindKmh(runway.getHeadingDegrees());
            if (crosswind >= 55) {
                severity = severity.max(WeatherSeverity.SEVERE);
                alerts.add(alert(snapshot, WeatherSeverity.SEVERE, "Severe crosswind on " + runway.getName(), airport.getId(), runway.getId(), ""));
            } else if (crosswind >= 35) {
                severity = severity.max(WeatherSeverity.CAUTION);
                alerts.add(alert(snapshot, WeatherSeverity.CAUTION, "Crosswind caution on " + runway.getName(), airport.getId(), runway.getId(), ""));
            }
        }
        if (alerts.isEmpty()) {
            alerts.add(alert(snapshot, WeatherSeverity.NORMAL, "Weather normal for airport operations", airport.getId(), "", ""));
        }
        return alerts;
    }

    private WeatherAlert alert(WeatherSnapshot snapshot, WeatherSeverity severity, String message, String airportId, String runwayId, String flightId) {
        return new WeatherAlert(snapshot.observedAt(), severity, message, airportId, runwayId, flightId);
    }
}
