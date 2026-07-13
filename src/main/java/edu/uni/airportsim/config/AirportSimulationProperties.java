package edu.uni.airportsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties(prefix = "airport-simulation")
public class AirportSimulationProperties {
    private Path dataDir = Path.of("data");
    private String defaultAirportCode = "LIS";
    private boolean autoStart = true;
    private Duration tickInterval = Duration.ofSeconds(1);
    private int demoFlightCount = 12;
    private int flightSeedLimit = 24;
    private String trafficProfile = "BUSY";
    private int targetDailyFlights = 220;
    private double passengerLoadFactor = 0.82;
    private double bagRate = 0.72;
    private boolean useOpenFlights = true;

    public Path getDataDir() {
        return dataDir;
    }

    public void setDataDir(Path dataDir) {
        this.dataDir = dataDir;
    }

    public String getDefaultAirportCode() {
        return defaultAirportCode;
    }

    public void setDefaultAirportCode(String defaultAirportCode) {
        this.defaultAirportCode = defaultAirportCode;
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

    public String getTrafficProfile() {
        return trafficProfile;
    }

    public void setTrafficProfile(String trafficProfile) {
        this.trafficProfile = trafficProfile;
    }

    public int getTargetDailyFlights() {
        return targetDailyFlights;
    }

    public void setTargetDailyFlights(int targetDailyFlights) {
        this.targetDailyFlights = targetDailyFlights;
    }

    public double getPassengerLoadFactor() {
        return passengerLoadFactor;
    }

    public void setPassengerLoadFactor(double passengerLoadFactor) {
        this.passengerLoadFactor = passengerLoadFactor;
    }

    public double getBagRate() {
        return bagRate;
    }

    public void setBagRate(double bagRate) {
        this.bagRate = bagRate;
    }

    public boolean isUseOpenFlights() {
        return useOpenFlights;
    }

    public void setUseOpenFlights(boolean useOpenFlights) {
        this.useOpenFlights = useOpenFlights;
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
