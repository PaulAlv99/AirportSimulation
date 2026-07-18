package edu.uni.airportsim.runtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record SimulationSettingsRequest(
        @NotNull LocalTime operatingStartTime,
        @NotNull LocalTime operatingEndTime,
        @NotBlank String trafficProfile,
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
