package edu.uni.airportsim.runtime;

public record ImportCounts(
        long countries,
        long regions,
        long airports,
        long runways,
        long navaids,
        long weatherSnapshots,
        long flightTemplates,
        long openFlightAirlines,
        long openFlightRoutes,
        long openFlightPlanes,
        long demoFlights,
        long passengers,
        long baggage,
        long gates,
        long groundOperations,
        long events
) {
}
