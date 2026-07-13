# Airport Simulation

Airport Simulation is now a Spring Boot web app with a live dashboard backed by PostgreSQL. It still keeps the old CLI mode available with `--cli`, but the default runtime is the browser UI.

## What it does

- imports CSV data from `data/import/`
- imports OpenFlights route, airline, and aircraft templates from `data/import/openflights/`
- seeds PostgreSQL through Flyway and a startup bootstrapper
- runs the simulation independently on a scheduler
- exposes a browser operations dashboard at `/projects/airport-simulation/`
- generates route-aware flights, passengers, baggage, gates, ground tasks, and queues
- keeps logs under `logs/` and save files under `saves/`

## Stack

- Java 25
- Spring Boot 3.5.16
- PostgreSQL 17
- Maven
- Docker and Docker Compose

## Data

The app reads these files from `data/import/`:

- `countries.csv`
- `regions.csv`
- `airports.csv`
- `runways.csv`
- `navaids.csv`
- `airlines_flights_data.csv`
- `weather.csv` if present
- `openflights/routes.dat`
- `openflights/airlines.dat`
- `openflights/planes.dat`

If `weather.csv` is missing, the app creates a demo weather snapshot automatically so the dashboard still shows live conditions.

OpenFlights data is used as historical route-template data, not live schedule truth. See `data/import/openflights/README.md`.
To refresh those files manually, run:

```powershell
.\scripts\download-openflights.ps1 -Apply
```

## Run locally

```bash
mvn test
mvn package
java -jar target/airport-simulation-1.0.0.jar
```

Open the app at:

`http://localhost:8080/projects/airport-simulation/`

CLI mode is still available:

```bash
java -jar target/airport-simulation-1.0.0.jar --cli
```

## Run with Docker

```bash
docker network create proxy
docker compose up --build
```

The compose stack starts PostgreSQL and the app together. It is meant to sit behind a shared reverse proxy on the external `proxy` network, so the app container only exposes port `8080` internally. The application expects its data mount at `/app/data`, so the repository `data/` directory is mounted into the container.
Set `AIRPORT_SIMULATION_DEFAULT_AIRPORT_CODE` if you want a different default airport than the bundled `LIS`.

Useful runtime variables:

- `AIRPORT_SIMULATION_TRAFFIC_PROFILE=BUSY`
- `AIRPORT_SIMULATION_TARGET_DAILY_FLIGHTS=220`
- `AIRPORT_SIMULATION_PASSENGER_LOAD_FACTOR=0.82`
- `AIRPORT_SIMULATION_BAG_RATE=0.72`
- `AIRPORT_SIMULATION_USE_OPENFLIGHTS=true`
- `AIRPORT_SIMULATION_RANDOM_SEED=0` uses live randomness; set a positive number for repeatable generated days
- `AIRPORT_SIMULATION_DELAY_PROBABILITY=0.08`
- `AIRPORT_SIMULATION_BAGGAGE_EXCEPTION_PROBABILITY=0.012`
- `AIRPORT_SIMULATION_PASSENGER_NO_SHOW_PROBABILITY=0.035`
- `AIRPORT_SIMULATION_GROUND_JITTER_MINUTES=12`

## Notes

- The dashboard polls the backend once per second.
- The simulation starts automatically by default.
- Flight loads, bag outcomes, no-shows, ground-task timing, and tick incidents use bounded probabilistic generation.
- The web UI uses relative paths so it can live behind a nested ProjectsShowcase route.
