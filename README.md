# Airport Simulation

Airport Simulation is now a Spring Boot web app with a live dashboard backed by PostgreSQL. It still keeps the old CLI mode available with `--cli`, but the default runtime is the browser UI.

## What it does

- imports CSV data from `data/import/`
- seeds PostgreSQL through Flyway and a startup bootstrapper
- runs the simulation independently on a scheduler
- exposes a browser dashboard at `/airport-simulation/`
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

If `weather.csv` is missing, the app creates a demo weather snapshot automatically so the dashboard still shows live conditions.

## Run locally

```bash
mvn test
mvn package
java -jar target/airport-simulation-1.0.0.jar
```

Open the app at:

`http://localhost:8080/airport-simulation/`

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

## Notes

- The dashboard polls the backend once per second.
- The simulation starts automatically by default.
- The web UI uses relative paths so it can live behind a nested ProjectsShowcase route.
