package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;
import java.util.List;

public record SimulationSnapshot(
        String lifecycleState,
        boolean running,
        LocalDateTime simulatedTime,
        String multiplier,
        AirportView airport,
        WeatherView weather,
        ImportCounts counts,
        OperationSummary operations,
        List<FlightView> flights,
        List<EventView> events
) {
}
