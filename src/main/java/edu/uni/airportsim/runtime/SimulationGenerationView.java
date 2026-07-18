package edu.uni.airportsim.runtime;

import java.time.LocalDateTime;

public record SimulationGenerationView(
        boolean open,
        LocalDateTime generationCursor,
        LocalDateTime generationHorizonEnd,
        LocalDateTime nextOpeningAt,
        long runSeed,
        long generatedFlights,
        long pendingFlights,
        int retentionDays
) {
}
