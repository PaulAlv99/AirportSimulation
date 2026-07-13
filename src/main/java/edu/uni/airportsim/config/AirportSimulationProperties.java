package edu.uni.airportsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "airport-simulation")
public class AirportSimulationProperties {
    private Path dataDir = Path.of("data");
    private String contextPath = "/projects/airport-simulation";
    private boolean autoStart = true;
    private Duration tickInterval = Duration.ofSeconds(1);
    private int demoFlightCount = 12;
    private int flightSeedLimit = 24;

    public Path getDataDir() {
        return dataDir;
    }

    public void setDataDir(Path dataDir) {
        this.dataDir = dataDir;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public Duration getTickInterval() {
        return tickInterval;
    }

    public void setTickInterval(Duration tickInterval) {
        this.tickInterval = tickInterval;
    }

    public int getDemoFlightCount() {
        return demoFlightCount;
    }

    public void setDemoFlightCount(int demoFlightCount) {
        this.demoFlightCount = demoFlightCount;
    }

    public int getFlightSeedLimit() {
        return flightSeedLimit;
    }

    public void setFlightSeedLimit(int flightSeedLimit) {
        this.flightSeedLimit = flightSeedLimit;
    }

    public Path importDirectory() {
        return dataDir.resolve("import");
    }

    public Path generatedDirectory() {
        return dataDir.resolve("generated");
    }

    public Path logsDirectory() {
        return Path.of("logs");
    }

    public Path savesDirectory() {
        return Path.of("saves");
    }
}
