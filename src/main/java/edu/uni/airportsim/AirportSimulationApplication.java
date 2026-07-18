package edu.uni.airportsim;

import edu.uni.airportsim.config.AirportSimulationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AirportSimulationProperties.class)
public class AirportSimulationApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirportSimulationApplication.class, args);
    }

    @Bean
    Clock airportSimulationClock() {
        return Clock.systemDefaultZone();
    }
}
