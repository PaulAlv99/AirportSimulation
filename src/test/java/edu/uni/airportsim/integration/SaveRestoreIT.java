package edu.uni.airportsim.integration;

import edu.uni.airportsim.domain.Address;
import edu.uni.airportsim.domain.Airport;
import edu.uni.airportsim.persistence.SaveGameService;
import edu.uni.airportsim.simulation.SimulationLifecycleState;
import edu.uni.airportsim.simulation.SimulationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveRestoreIT {
    @TempDir
    Path tempDir;

    @Test
    void saveFileCanBeRestoredByIntegrationFlow() {
        SimulationState state = new SimulationState();
        state.setLifecycleState(SimulationLifecycleState.LOADED);
        state.setAirport(new Airport("APT-OPO", "Francisco Sa Carneiro Airport", "OPO", new Address("Porto", "Portugal")));

        SaveGameService service = new SaveGameService();
        Path saveFile = tempDir.resolve("state.json");
        service.save(state, saveFile);

        SimulationState restored = service.restore(saveFile);

        assertEquals("OPO", restored.getAirport().getCode());
        assertEquals(SimulationLifecycleState.LOADED.code(), restored.getLifecycleState().code());
    }
}
