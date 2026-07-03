package edu.uni.airportsim.simulation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public class SimulationClock {
    private final Clock clock;
    private Instant realStartedAt;
    private LocalDateTime simulatedStartedAt;
    private TimeMultiplier multiplier = TimeMultiplier.X1;

    public SimulationClock() {
        this(Clock.systemDefaultZone(), LocalDateTime.now());
    }

    public SimulationClock(Clock clock, LocalDateTime simulatedStartedAt) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.realStartedAt = clock.instant();
        this.simulatedStartedAt = Objects.requireNonNull(simulatedStartedAt, "simulatedStartedAt");
    }

    public Instant getRealStartedAt() {
        return realStartedAt;
    }

    public LocalDateTime getSimulatedStartedAt() {
        return simulatedStartedAt;
    }

    public TimeMultiplier getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(TimeMultiplier multiplier) {
        LocalDateTime currentTime = now();
        this.realStartedAt = clock.instant();
        this.simulatedStartedAt = currentTime;
        this.multiplier = Objects.requireNonNull(multiplier, "multiplier");
    }

    public Instant realNow() {
        return clock.instant();
    }

    public LocalDateTime now() {
        return currentSimulatedTime(clock.instant());
    }

    public LocalDateTime currentSimulatedTime(Instant realNow) {
        Duration elapsedRealTime = Duration.between(realStartedAt, realNow);
        return simulatedStartedAt.plus(elapsedRealTime.multipliedBy(multiplier.factor()));
    }

    public ZoneId zone() {
        return clock.getZone();
    }
}
