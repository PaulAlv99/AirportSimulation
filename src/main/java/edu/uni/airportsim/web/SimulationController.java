package edu.uni.airportsim.web;

import edu.uni.airportsim.runtime.AirportOption;
import edu.uni.airportsim.runtime.BaggageView;
import edu.uni.airportsim.runtime.GateView;
import edu.uni.airportsim.runtime.GroundOperationView;
import edu.uni.airportsim.runtime.OperationSummary;
import edu.uni.airportsim.runtime.PassengerManifestView;
import edu.uni.airportsim.runtime.SimulationFacade;
import edu.uni.airportsim.runtime.SimulationSnapshot;
import edu.uni.airportsim.runtime.WeatherInput;
import edu.uni.airportsim.runtime.WeatherView;
import edu.uni.airportsim.simulation.TimeMultiplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SimulationController {
    private final SimulationFacade facade;

    public SimulationController(SimulationFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/snapshot")
    public SimulationSnapshot snapshot() {
        return facade.snapshot();
    }

    @GetMapping("/airports")
    public List<AirportOption> airports(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", defaultValue = "40") @Range(min = 1, max = 100) int limit
    ) {
        return facade.airports(q, limit);
    }

    @PostMapping("/control/start")
    public ResponseEntity<Void> start() {
        facade.start();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/control/pause")
    public ResponseEntity<Void> pause() {
        facade.pause();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/control/reset")
    public ResponseEntity<Void> reset() {
        facade.reset();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/control/multiplier")
    public ResponseEntity<Void> multiplier(@Valid @RequestBody MultiplierRequest request) {
        facade.setMultiplier(TimeMultiplier.fromLabel(request.multiplier()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/airport/select")
    public ResponseEntity<Void> selectAirport(@Valid @RequestBody AirportRequest request) {
        facade.selectAirport(request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/weather/manual")
    public WeatherView manualWeather(@Valid @RequestBody WeatherRequest request) {
        return facade.updateWeather(request.toInput());
    }

    @PostMapping("/weather/fetch")
    public WeatherView fetchWeather() {
        return facade.fetchRealWeather();
    }

    @PostMapping("/import/reseed")
    public ResponseEntity<Void> reseed() {
        facade.reseed();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/operations/summary")
    public OperationSummary operationsSummary() {
        return facade.operationSummary();
    }

    @GetMapping("/operations/baggage")
    public List<BaggageView> baggage(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "flight", required = false) String flight,
            @RequestParam(name = "limit", defaultValue = "80") @Range(min = 1, max = 250) int limit
    ) {
        return facade.baggage(status, flight, limit);
    }

    @GetMapping("/operations/gates")
    public List<GateView> gates() {
        return facade.gates();
    }

    @GetMapping("/operations/ground")
    public List<GroundOperationView> ground() {
        return facade.groundOperations();
    }

    @PostMapping("/operations/disruption")
    public ResponseEntity<Void> disruption(@Valid @RequestBody DisruptionRequest request) {
        facade.applyDisruption(request.type(), request.severity(), request.durationMinutes(), request.gateCode());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/flights/{id}/control")
    public ResponseEntity<Void> flightControl(@PathVariable("id") long id, @Valid @RequestBody FlightControlRequest request) {
        facade.controlFlight(id, request.status(), request.delayMinutes(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/flights/{id}/passengers")
    public List<PassengerManifestView> flightPassengers(
            @PathVariable("id") long id,
            @RequestParam(name = "limit", defaultValue = "180") @Range(min = 1, max = 500) int limit
    ) {
        return facade.passengersForFlight(id, limit);
    }

    public record MultiplierRequest(@NotBlank String multiplier) {
    }

    public record AirportRequest(@NotBlank String code) {
    }

    public record WeatherRequest(
            @NotNull Double temperatureCelsius,
            @NotNull Double feelsLikeCelsius,
            @NotNull @PositiveOrZero Double windSpeedKmh,
            @NotNull @PositiveOrZero Double windGustKmh,
            @Range(min = 0, max = 359) int windDirectionDegrees,
            @PositiveOrZero double rainMmPerHour,
            @PositiveOrZero double snowMmPerHour,
            boolean hail,
            boolean thunderstorm,
            @PositiveOrZero int visibilityMeters,
            boolean fog,
            @Range(min = 0, max = 100) int cloudCoveragePercent,
            @PositiveOrZero int ceilingMeters,
            @NotBlank String cloudLabel,
            @NotBlank String runwaySurface,
            @NotBlank String severityCode
    ) {
        WeatherInput toInput() {
            return new WeatherInput(
                    temperatureCelsius,
                    feelsLikeCelsius,
                    windSpeedKmh,
                    windGustKmh,
                    windDirectionDegrees,
                    rainMmPerHour,
                    snowMmPerHour,
                    hail,
                    thunderstorm,
                    visibilityMeters,
                    fog,
                    cloudCoveragePercent,
                    ceilingMeters,
                    cloudLabel,
                    runwaySurface,
                    severityCode
            );
        }
    }

    public record DisruptionRequest(
            @NotBlank String type,
            @Range(min = 1, max = 5) int severity,
            @Range(min = 5, max = 240) int durationMinutes,
            String gateCode
    ) {
    }

    public record FlightControlRequest(
            @NotBlank String status,
            @PositiveOrZero Integer delayMinutes,
            String reason
    ) {
    }
}
