package edu.uni.airportsim.web;

import edu.uni.airportsim.runtime.SimulationFacade;
import edu.uni.airportsim.runtime.SimulationSnapshot;
import edu.uni.airportsim.simulation.TimeMultiplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/import/reseed")
    public ResponseEntity<Void> reseed() {
        facade.reseed();
        return ResponseEntity.noContent().build();
    }

    public record MultiplierRequest(@NotBlank String multiplier) {
    }
}
