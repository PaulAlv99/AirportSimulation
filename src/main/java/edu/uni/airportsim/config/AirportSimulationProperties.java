package edu.uni.airportsim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;

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
    private long randomSeed = 0L;
    private LocalTime operatingStartTime = LocalTime.of(5, 0);
    private LocalTime operatingEndTime = LocalTime.of(23, 0);
    private double staffingMultiplier = 1.0;
    private int planningHorizonMinutes = 180;
    private int retentionDays = 7;
    private double delayProbability = 0.08;
    private double baggageExceptionProbability = 0.012;
    private double passengerNoShowProbability = 0.035;
    private int groundJitterMinutes = 12;

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

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public LocalTime getOperatingStartTime() {
        return operatingStartTime;
    }

    public void setOperatingStartTime(LocalTime operatingStartTime) {
        this.operatingStartTime = operatingStartTime;
    }

    public LocalTime getOperatingEndTime() {
        return operatingEndTime;
    }

    public void setOperatingEndTime(LocalTime operatingEndTime) {
        this.operatingEndTime = operatingEndTime;
    }

    public double getStaffingMultiplier() {
        return staffingMultiplier;
    }

    public void setStaffingMultiplier(double staffingMultiplier) {
        this.staffingMultiplier = staffingMultiplier;
    }

    public int getPlanningHorizonMinutes() {
        return planningHorizonMinutes;
    }

    public void setPlanningHorizonMinutes(int planningHorizonMinutes) {
        this.planningHorizonMinutes = planningHorizonMinutes;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public double getDelayProbability() {
        return delayProbability;
    }

    public void setDelayProbability(double delayProbability) {
        this.delayProbability = delayProbability;
    }

    public double getBaggageExceptionProbability() {
        return baggageExceptionProbability;
    }

    public void setBaggageExceptionProbability(double baggageExceptionProbability) {
        this.baggageExceptionProbability = baggageExceptionProbability;
    }

    public double getPassengerNoShowProbability() {
        return passengerNoShowProbability;
    }

    public void setPassengerNoShowProbability(double passengerNoShowProbability) {
        this.passengerNoShowProbability = passengerNoShowProbability;
    }

    public int getGroundJitterMinutes() {
        return groundJitterMinutes;
    }

    public void setGroundJitterMinutes(int groundJitterMinutes) {
        this.groundJitterMinutes = groundJitterMinutes;
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
