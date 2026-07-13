package edu.uni.airportsim.web;

import edu.uni.airportsim.runtime.AirportOption;
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
}
