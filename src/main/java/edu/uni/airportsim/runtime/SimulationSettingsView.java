package edu.uni.airportsim.runtime;

import java.time.LocalTime;

public record SimulationSettingsView(
        LocalTime operatingStartTime,
        LocalTime operatingEndTime,
        String trafficProfile,
        int targetDailyFlights,
        double passengerLoadFactor,
        double bagRate,
        double staffingMultiplier,
        double delayProbability,
        double baggageExceptionProbability,
        double passengerNoShowProbability,
        int groundJitterMinutes,
        int planningHorizonMinutes,
        int retentionDays,
        long randomSeed
) {
}
