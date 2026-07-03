package edu.uni.airportsim.cli;

import edu.uni.airportsim.persistence.SaveGameService;
import edu.uni.airportsim.simulation.SimulationState;

import java.nio.file.Path;

public class SaveRestoreMenu {
    private final SaveGameService saveGameService = new SaveGameService();
    private final Path saveFile;

    public SaveRestoreMenu(Path saveFile) {
        this.saveFile = saveFile;
    }

    public void save(SimulationState state) {
        saveGameService.save(state, saveFile);
    }

    public SimulationState restore() {
        return saveGameService.restore(saveFile);
    }
}
