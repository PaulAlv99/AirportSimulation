package edu.uni.airportsim.app;

import edu.uni.airportsim.AirportSimulationApplication;
import org.springframework.boot.SpringApplication;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (Arrays.stream(args).anyMatch("--cli"::equals)) {
            new AirportSimulationApp().run();
            return;
        }

        SpringApplication.run(AirportSimulationApplication.class, args);
    }
}
