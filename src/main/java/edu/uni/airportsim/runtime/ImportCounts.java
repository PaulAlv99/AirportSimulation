package edu.uni.airportsim.runtime;

public record ImportCounts(
        long countries,
        long regions,
        long airports,
        long runways,
        long navaids,
        long weatherSnapshots,
        long flightTemplates,
        long demoFlights,
        long events
) {
}
