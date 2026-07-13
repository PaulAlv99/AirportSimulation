package edu.uni.airportsim.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import edu.uni.airportsim.config.AirportSimulationProperties;
import edu.uni.airportsim.data.CsvCopyLoader;
import edu.uni.airportsim.logging.LogLevel;
import edu.uni.airportsim.logging.SimulationLogger;
import edu.uni.airportsim.simulation.TimeMultiplier;
import edu.uni.airportsim.weather.WeatherSeverity;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class SimulationFacade implements ApplicationRunner {
    private static final String STATE_ID = "1";

    private final JdbcTemplate jdbcTemplate;
    private final CsvCopyLoader copyLoader;
    private final AirportSimulationProperties properties;
    private final SimulationLogger logger;
    private final RestClient weatherClient;
    private final Clock clock = Clock.systemDefaultZone();
    private volatile boolean shuttingDown;

    public SimulationFacade(JdbcTemplate jdbcTemplate, CsvCopyLoader copyLoader, AirportSimulationProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.copyLoader = copyLoader;
        this.properties = properties;
        this.logger = new SimulationLogger(properties.logsDirectory());
        this.weatherClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrap();
    }

    @Transactional
    public void bootstrap() {
        if (!isBootstrapped()) {
            importAllData();
            resetSimulationState();
            markBootstrapped();
        } else {
            ensureRuntimeState();
        }

        if (properties.isAutoStart()) {
            start();
        }
    }

    @Transactional
    public void reseed() {
        truncateAllTables();
        importAllData();
        resetSimulationState();
        markBootstrapped();
    }

    @Transactional
    public void reset() {
        resetSimulationState();
    }

    @Transactional
    public void start() {
        SimulationStateRow state = requireState();
        jdbcTemplate.update("""
                update simulation_state
                set lifecycle_state = ?, running = ?, updated_at = ?
                where id = 1
                """,
                "RUNNING",
                true,
                LocalDateTime.now(clock));
        logger.log(LogLevel.INFO, "SIMULATION", "Simulation started");
        if (!state.running()) {
            insertEvent("INFO", "SIMULATION", "Simulation started");
        }
    }

    @Transactional
    public void pause() {
        SimulationStateRow state = requireState();
        jdbcTemplate.update("""
                update simulation_state
                set lifecycle_state = ?, running = ?, updated_at = ?
                where id = 1
                """,
                "PAUSED",
                false,
                LocalDateTime.now(clock));
        logger.log(LogLevel.INFO, "SIMULATION", "Simulation paused");
        if (state.running()) {
            insertEvent("INFO", "SIMULATION", "Simulation paused");
        }
    }

    @Transactional
    public void setMultiplier(TimeMultiplier multiplier) {
        Objects.requireNonNull(multiplier, "multiplier");
        requireState();
        jdbcTemplate.update("""
                update simulation_state
                set multiplier = ?, updated_at = ?
                where id = 1
                """,
                multiplier.label(),
                LocalDateTime.now(clock));
        insertEvent("INFO", "CLOCK", "Multiplier changed to " + multiplier.label());
    }

    @PreDestroy
    public void stopScheduledWork() {
        shuttingDown = true;
    }

    public List<AirportOption> airports(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String search = query == null ? "" : query.trim();
        if (search.isBlank()) {
            return queryAirportOptions("""
                            select a.id,
                                   coalesce(nullif(a.iata_code, ''), nullif(a.ident, ''), 'APT-' || a.id::text) as code,
                                   a.ident,
                                   a.name,
                                   coalesce(nullif(a.municipality, ''), a.name) as city,
                                   coalesce(nullif(a.iso_country, ''), 'UNKNOWN') as country,
                                   coalesce(nullif(a.type, ''), 'airport') as type,
                                   a.latitude_deg,
                                   a.longitude_deg,
                                   (
                                       select count(*)
                                       from import_runways r
                                       where r.airport_ref = a.id or r.airport_ident = a.ident
                                   ) as runways
                            from import_airports a
                            where coalesce(nullif(a.iata_code, ''), nullif(a.ident, '')) is not null
                            order by case when lower(coalesce(nullif(a.scheduled_service, ''), 'no')) = 'yes' then 0 else 1 end,
                                     case when nullif(a.iata_code, '') is not null then 0 else 1 end,
                                     a.name,
                                     a.id
                            limit ?
                            """,
                    safeLimit);
        }

        String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
        return queryAirportOptions("""
                        select a.id,
                               coalesce(nullif(a.iata_code, ''), nullif(a.ident, ''), 'APT-' || a.id::text) as code,
                               a.ident,
                               a.name,
                               coalesce(nullif(a.municipality, ''), a.name) as city,
                               coalesce(nullif(a.iso_country, ''), 'UNKNOWN') as country,
                               coalesce(nullif(a.type, ''), 'airport') as type,
                               a.latitude_deg,
                               a.longitude_deg,
                               (
                                   select count(*)
                                   from import_runways r
                                   where r.airport_ref = a.id or r.airport_ident = a.ident
                               ) as runways
                        from import_airports a
                        where coalesce(nullif(a.iata_code, ''), nullif(a.ident, '')) is not null
                          and (
                              coalesce(nullif(a.iata_code, ''), nullif(a.ident, ''), '') ilike ?
                              or coalesce(nullif(a.ident, ''), '') ilike ?
                              or a.name ilike ?
                              or coalesce(nullif(a.municipality, ''), '') ilike ?
                              or coalesce(nullif(a.iso_country, ''), '') ilike ?
                              or coalesce(nullif(a.type, ''), '') ilike ?
                          )
                        order by case when lower(coalesce(nullif(a.scheduled_service, ''), 'no')) = 'yes' then 0 else 1 end,
                                 case when nullif(a.iata_code, '') is not null then 0 else 1 end,
                                 a.name,
                                 a.id
                        limit ?
                        """,
                pattern, pattern, pattern, pattern, pattern, pattern, safeLimit);
    }

    @Transactional
    public void selectAirport(String airportCode) {
        String normalizedCode = normalizeRequiredCode(airportCode);
        AirportRow airport = airportByCode(normalizedCode);
        jdbcTemplate.update("""
                update simulation_state
                set active_airport_code = ?, updated_at = ?
                where id = 1
                """,
                airport.code(),
                LocalDateTime.now(clock));
        if (!hasWeatherFor(airport.code())) {
            seedSyntheticWeather(airport.code());
        }
        insertEvent("INFO", "AIRPORT", "Active airport changed to " + airport.code() + " - " + airport.name());
    }

    @Transactional
    public WeatherView updateWeather(WeatherInput input) {
        Objects.requireNonNull(input, "input");
        String airportCode = requireState().activeAirportCode();
        insertWeatherSnapshot(airportCode, normalizeWeatherInput(input));
        insertEvent("INFO", "WEATHER", "Manual weather update applied at " + airportCode);
        return currentWeather(airportCode);
    }

    @Transactional
    public WeatherView fetchRealWeather() {
        SimulationStateRow state = requireState();
        AirportRow airport = airportByCode(state.activeAirportCode());
        if (airport.latitude() == null || airport.longitude() == null) {
            throw new IllegalArgumentException("Active airport does not have coordinates for weather lookup");
        }

        JsonNode current = fetchCurrentWeather(airport.latitude(), airport.longitude());
        WeatherInput input = weatherInputFromApi(current);
        insertWeatherSnapshot(airport.code(), input);
        insertEvent("INFO", "WEATHER", "Fetched live weather for " + airport.code() + " from Open-Meteo");
        return currentWeather(airport.code());
    }

    @Scheduled(fixedRateString = "${airport-simulation.tick-interval:1s}")
    @Transactional
    public void tick() {
        if (shuttingDown) {
            return;
        }
        SimulationStateRow state = loadState();
        if (state == null || !state.running()) {
            return;
        }

        TimeMultiplier multiplier = TimeMultiplier.fromLabel(state.multiplier());
        long simulatedSeconds = Math.max(1L, multiplier.factor());
        LocalDateTime nextTime = state.simulatedTime().plusSeconds(simulatedSeconds);
        jdbcTemplate.update("""
                update simulation_state
                set simulated_time = ?, updated_at = ?, lifecycle_state = ?
                where id = 1
                """,
                nextTime,
                LocalDateTime.now(clock),
                "RUNNING");

        WeatherView weather = currentWeather(state.activeAirportCode());
        WeatherSeverity severity = WeatherSeverity.fromCode(weather.severityCode());
        List<FlightRow> flights = loadFlights();
        for (FlightRow flight : flights) {
            updateFlightState(flight, nextTime, severity);
        }
    }

    public SimulationSnapshot snapshot() {
        SimulationStateRow state = requireState();
        AirportView airport = activeAirport(state.activeAirportCode());
        WeatherView weather = currentWeather(state.activeAirportCode());
        ImportCounts counts = new ImportCounts(
                count("import_countries"),
                count("import_regions"),
                count("import_airports"),
                count("import_runways"),
                count("import_navaids"),
                count("import_weather_snapshots"),
                count("import_airline_flights"),
                count("simulation_flights"),
                count("simulation_events")
        );
        List<FlightView> flights = loadFlights().stream()
                .map(this::toFlightView)
                .toList();
        List<EventView> events = loadEvents(12).stream()
                .map(this::toEventView)
                .toList();
        return new SimulationSnapshot(
                state.lifecycleState(),
                state.running(),
                state.simulatedTime(),
                state.multiplier(),
                airport,
                weather,
                counts,
                flights,
                events
        );
    }

    private void ensureRuntimeState() {
        if (count("import_airports") == 0 || count("import_airline_flights") == 0) {
            importAllData();
            markBootstrapped();
        }
        if (loadState() == null || count("simulation_flights") == 0) {
            resetSimulationState();
        }
    }

    private void importAllData() {
        copyLoader.copyIfPresent(path("countries.csv"), "import_countries");
        copyLoader.copyIfPresent(path("regions.csv"), "import_regions");
        copyLoader.copyIfPresent(path("airports.csv"), "import_airports");
        copyLoader.copyIfPresent(path("runways.csv"), "import_runways");
        copyLoader.copyIfPresent(path("navaids.csv"), "import_navaids");
        copyLoader.copyIfPresent(path("airlines_flights_data.csv"), "import_airline_flights");
        copyLoader.copyIfPresent(path("weather.csv"), "import_weather_snapshots");

        if (count("import_weather_snapshots") == 0) {
            seedSyntheticWeather(activeAirportCode());
        }
    }

    private void resetSimulationState() {
        jdbcTemplate.update("""
                delete from simulation_flights
                """);
        jdbcTemplate.update("""
                delete from simulation_events
                """);

        String activeAirportCode = activeAirportCode();
        LocalDateTime simulatedTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES).plusMinutes(2);
        jdbcTemplate.update("""
                insert into simulation_state (id, lifecycle_state, simulated_time, multiplier, running, active_airport_code, updated_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (id) do update set
                    lifecycle_state = excluded.lifecycle_state,
                    simulated_time = excluded.simulated_time,
                    multiplier = excluded.multiplier,
                    running = excluded.running,
                    active_airport_code = excluded.active_airport_code,
                    updated_at = excluded.updated_at
                """,
                1,
                "LOADED",
                simulatedTime,
                TimeMultiplier.X1.label(),
                false,
                activeAirportCode,
                LocalDateTime.now(clock));

        seedDemoFlights(simulatedTime);
        if (!hasWeatherFor(activeAirportCode)) {
            seedSyntheticWeather(activeAirportCode);
        }
        insertEvent("INFO", "SIMULATION", "Simulation reset");
    }

    private void seedDemoFlights(LocalDateTime baseTime) {
        List<TemplateFlightRow> sourceRows = loadTemplateFlights(properties.getFlightSeedLimit());
        if (sourceRows.isEmpty()) {
            sourceRows = fallbackTemplateFlights();
        }

        List<Object[]> rows = new ArrayList<>();
        for (int index = 0; index < sourceRows.size(); index++) {
            TemplateFlightRow template = sourceRows.get(index);
            LocalDateTime departure = baseTime.plusMinutes(index * 7L + (template.daysLeft() % 4));
            long durationMinutes = Math.max(45L, Math.round(template.durationHours() * 60.0));
            LocalDateTime arrival = departure.plusMinutes(durationMinutes);
            rows.add(new Object[] {
                    template.rowId(),
                    template.flightNumber(),
                    template.airline(),
                    template.sourceCity(),
                    template.destinationCity(),
                    departure,
                    arrival,
                    "SCHEDULED",
                    0,
                    gateFor(index),
                    runwayFor(index),
                    null,
                    LocalDateTime.now(clock)
            });
        }

        jdbcTemplate.batchUpdate("""
                insert into simulation_flights (
                    source_row_id,
                    flight_number,
                    airline,
                    origin_label,
                    destination_label,
                    departure_time,
                    arrival_time,
                    status,
                    delay_minutes,
                    gate,
                    runway,
                    weather_notes,
                    last_updated
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);
    }

    private void seedSyntheticWeather(String airportCode) {
        insertWeatherSnapshot(airportCode, new WeatherInput(
                17.0,
                15.0,
                34.0,
                49.0,
                280,
                3.5,
                0.0,
                false,
                false,
                2400,
                false,
                78,
                900,
                "Low Clouds",
                "WET",
                WeatherSeverity.CAUTION.code()
        ));
    }

    private void updateFlightState(FlightRow flight, LocalDateTime currentTime, WeatherSeverity weatherSeverity) {
        String effectiveStatus = statusFor(flight, currentTime);
        int delayMinutes = flight.delayMinutes();

        if (delayMinutes == 0
                && weatherSeverity.rank() >= WeatherSeverity.CAUTION.rank()
                && "BOARDING".equals(effectiveStatus)) {
            delayMinutes = weatherSeverity.rank() >= WeatherSeverity.SEVERE.rank() ? 20 : 10;
            effectiveStatus = "DELAYED";
            jdbcTemplate.update("""
                    update simulation_flights
                    set delay_minutes = ?, status = ?, weather_notes = ?, last_updated = ?
                    where id = ?
                    """,
                    delayMinutes,
                    effectiveStatus,
                    "Weather delay due to " + weatherSeverity.label().toLowerCase(Locale.ROOT),
                    LocalDateTime.now(clock),
                    flight.id());
            insertEvent("WARN", "WEATHER", flight.flightNumber() + " delayed " + delayMinutes + " minutes by weather");
            return;
        }

        if (!effectiveStatus.equals(flight.status())) {
            jdbcTemplate.update("""
                    update simulation_flights
                    set status = ?, last_updated = ?
                    where id = ?
                    """,
                    effectiveStatus,
                    LocalDateTime.now(clock),
                    flight.id());
            if ("ARRIVED".equals(effectiveStatus) && currentTime.isAfter(flight.arrivalTime().plusMinutes(delayMinutes))) {
                insertEvent("INFO", "FLIGHT", flight.flightNumber() + " arrived at " + flight.destinationLabel());
            } else {
                insertEvent("INFO", "FLIGHT", flight.flightNumber() + " moved to " + effectiveStatus);
            }
        }
    }

    private String statusFor(FlightRow flight, LocalDateTime currentTime) {
        LocalDateTime departure = flight.departureTime().plusMinutes(flight.delayMinutes());
        LocalDateTime arrival = flight.arrivalTime().plusMinutes(flight.delayMinutes());
        if (currentTime.isBefore(departure.minusMinutes(30))) {
            return "SCHEDULED";
        }
        if (currentTime.isBefore(departure.minusMinutes(15))) {
            return "CHECK_IN_OPEN";
        }
        if (currentTime.isBefore(departure)) {
            return flight.delayMinutes() > 0 ? "DELAYED" : "BOARDING";
        }
        if (currentTime.isBefore(arrival)) {
            return "DEPARTED";
        }
        return "ARRIVED";
    }

    private List<TemplateFlightRow> loadTemplateFlights(int limit) {
        return jdbcTemplate.query("""
                select row_id, airline, flight, source_city, destination_city, duration, days_left
                from import_airline_flights
                order by row_id
                limit ?
                """,
                (rs, rowNum) -> new TemplateFlightRow(
                        rs.getLong("row_id"),
                        rs.getString("airline"),
                        rs.getString("flight"),
                        rs.getString("source_city"),
                        rs.getString("destination_city"),
                        rs.getDouble("duration"),
                        rs.getInt("days_left")
                ),
                limit);
    }

    private List<TemplateFlightRow> fallbackTemplateFlights() {
        String airportCode = activeAirportCode();
        return List.of(
                new TemplateFlightRow(1L, "Airport Simulation", "SIM100", airportCode, "Arrival Zone", 1.2, 0),
                new TemplateFlightRow(2L, "Airport Simulation", "SIM200", "Arrival Zone", airportCode, 1.4, 1),
                new TemplateFlightRow(3L, "Airport Simulation", "SIM300", airportCode, "Maintenance Stand", 0.9, 0)
        );
    }

    private List<FlightRow> loadFlights() {
        return jdbcTemplate.query("""
                select id, source_row_id, flight_number, airline, origin_label, destination_label,
                       departure_time, arrival_time, status, delay_minutes, gate, runway, weather_notes
                from simulation_flights
                order by departure_time, id
                """,
                (rs, rowNum) -> new FlightRow(
                        rs.getLong("id"),
                        rs.getLong("source_row_id"),
                        rs.getString("flight_number"),
                        rs.getString("airline"),
                        rs.getString("origin_label"),
                        rs.getString("destination_label"),
                        rs.getObject("departure_time", LocalDateTime.class),
                        rs.getObject("arrival_time", LocalDateTime.class),
                        rs.getString("status"),
                        rs.getInt("delay_minutes"),
                        rs.getString("gate"),
                        rs.getString("runway"),
                        rs.getString("weather_notes")
                ));
    }

    private List<EventRow> loadEvents(int limit) {
        return jdbcTemplate.query("""
                select occurred_at, level, category, message
                from simulation_events
                order by occurred_at desc, id desc
                limit ?
                """,
                (rs, rowNum) -> new EventRow(
                        rs.getObject("occurred_at", LocalDateTime.class),
                        rs.getString("level"),
                        rs.getString("category"),
                        rs.getString("message")
                ),
                limit);
    }

    private FlightView toFlightView(FlightRow row) {
        return new FlightView(
                row.id(),
                row.flightNumber(),
                row.airline(),
                row.originLabel(),
                row.destinationLabel(),
                row.departureTime(),
                row.arrivalTime(),
                row.status(),
                row.delayMinutes(),
                row.gate(),
                row.runway(),
                row.weatherNotes()
        );
    }

    private EventView toEventView(EventRow row) {
        return new EventView(row.occurredAt(), row.level(), row.category(), row.message());
    }

    private WeatherView currentWeather(String airportCode) {
        List<WeatherRow> rows = jdbcTemplate.query("""
                        select airport_id, observed_at, temperature_celsius, feels_like_celsius, wind_speed_kmh,
                               wind_gust_kmh, wind_direction_degrees, rain_mm_per_hour, snow_mm_per_hour,
                               hail, thunderstorm, visibility_meters, fog, cloud_coverage_percent,
                               ceiling_meters, cloud_label, runway_surface, weather_severity
                        from import_weather_snapshots
                        where airport_id = ?
                        order by observed_at desc
                        limit 1
                        """,
                (rs, rowNum) -> new WeatherRow(
                        rs.getString("airport_id"),
                        rs.getObject("observed_at", LocalDateTime.class),
                        rs.getDouble("temperature_celsius"),
                        rs.getDouble("feels_like_celsius"),
                        rs.getDouble("wind_speed_kmh"),
                        rs.getDouble("wind_gust_kmh"),
                        rs.getInt("wind_direction_degrees"),
                        rs.getDouble("rain_mm_per_hour"),
                        rs.getDouble("snow_mm_per_hour"),
                        rs.getBoolean("hail"),
                        rs.getBoolean("thunderstorm"),
                        rs.getInt("visibility_meters"),
                        rs.getBoolean("fog"),
                        rs.getInt("cloud_coverage_percent"),
                        rs.getInt("ceiling_meters"),
                        rs.getString("cloud_label"),
                        rs.getString("runway_surface"),
                        rs.getString("weather_severity")
                ),
                airportCode);
        if (rows.isEmpty()) {
            rows = jdbcTemplate.query("""
                            select airport_id, observed_at, temperature_celsius, feels_like_celsius, wind_speed_kmh,
                                   wind_gust_kmh, wind_direction_degrees, rain_mm_per_hour, snow_mm_per_hour,
                                   hail, thunderstorm, visibility_meters, fog, cloud_coverage_percent,
                                   ceiling_meters, cloud_label, runway_surface, weather_severity
                            from import_weather_snapshots
                            order by observed_at desc
                            limit 1
                            """,
                    (rs, rowNum) -> new WeatherRow(
                            rs.getString("airport_id"),
                            rs.getObject("observed_at", LocalDateTime.class),
                            rs.getDouble("temperature_celsius"),
                            rs.getDouble("feels_like_celsius"),
                            rs.getDouble("wind_speed_kmh"),
                            rs.getDouble("wind_gust_kmh"),
                            rs.getInt("wind_direction_degrees"),
                            rs.getDouble("rain_mm_per_hour"),
                            rs.getDouble("snow_mm_per_hour"),
                            rs.getBoolean("hail"),
                            rs.getBoolean("thunderstorm"),
                            rs.getInt("visibility_meters"),
                            rs.getBoolean("fog"),
                            rs.getInt("cloud_coverage_percent"),
                            rs.getInt("ceiling_meters"),
                            rs.getString("cloud_label"),
                            rs.getString("runway_surface"),
                            rs.getString("weather_severity")
                    ));
        }
        if (rows.isEmpty()) {
            return new WeatherView(
                    airportCode,
                    LocalDateTime.now(clock),
                    20.0,
                    20.0,
                    0.0,
                    0.0,
                    0,
                    0.0,
                    0.0,
                    false,
                    false,
                    10000,
                    false,
                    0,
                    3000,
                    "Clear",
                    "DRY",
                    WeatherSeverity.NORMAL.code(),
                    WeatherSeverity.NORMAL.label(),
                    "Conditions normal"
            );
        }

        WeatherRow row = rows.getFirst();
        WeatherSeverity severity = WeatherSeverity.fromCode(row.severityCode());
        return new WeatherView(
                row.airportId(),
                row.observedAt(),
                row.temperatureCelsius(),
                row.feelsLikeCelsius(),
                row.windSpeedKmh(),
                row.windGustKmh(),
                row.windDirectionDegrees(),
                row.rainMmPerHour(),
                row.snowMmPerHour(),
                row.hail(),
                row.thunderstorm(),
                row.visibilityMeters(),
                row.fog(),
                row.cloudCoveragePercent(),
                row.ceilingMeters(),
                row.cloudLabel(),
                row.runwaySurface(),
                severity.code(),
                severity.label(),
                weatherMessage(severity)
        );
    }

    private AirportView activeAirport(String airportCode) {
        List<AirportRow> airports = jdbcTemplate.query("""
                        select id, coalesce(nullif(iata_code, ''), nullif(ident, ''), 'APT-' || id::text) as code,
                               ident,
                               name,
                               coalesce(nullif(municipality, ''), name) as city,
                               coalesce(nullif(iso_country, ''), 'UNKNOWN') as country,
                               coalesce(nullif(type, ''), 'airport') as type,
                               latitude_deg,
                               longitude_deg
                        from import_airports
                        where coalesce(nullif(iata_code, ''), nullif(ident, '')) = ?
                           or iata_code = ?
                           or ident = ?
                        order by id
                        limit 1
                        """,
                (rs, rowNum) -> new AirportRow(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("ident"),
                        rs.getString("name"),
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getString("type"),
                        nullableDouble(rs, "latitude_deg"),
                        nullableDouble(rs, "longitude_deg")
                ),
                airportCode,
                airportCode,
                airportCode);
        AirportRow airport = airports.isEmpty() ? defaultAirport() : airports.getFirst();
        long runwayCount = jdbcTemplate.queryForObject("""
                select count(*)
                from import_runways
                where airport_ref = ? or airport_ident = ?
                """, Long.class, airport.id(), airport.ident());
        return new AirportView(
                airport.code(),
                airport.name(),
                airport.city(),
                airport.country(),
                runwayCount,
                airport.type(),
                airport.latitude(),
                airport.longitude()
        );
    }

    private AirportRow defaultAirport() {
        List<AirportRow> airports = queryAirportRows("""
                        select id,
                               coalesce(nullif(iata_code, ''), nullif(ident, ''), 'APT-' || id::text) as code,
                               ident,
                               name,
                               coalesce(nullif(municipality, ''), name) as city,
                               coalesce(nullif(iso_country, ''), 'UNKNOWN') as country,
                               coalesce(nullif(type, ''), 'airport') as type,
                               latitude_deg,
                               longitude_deg
                        from import_airports
                        where coalesce(nullif(iata_code, ''), nullif(ident, '')) is not null
                        order by case when lower(coalesce(nullif(scheduled_service, ''), 'no')) = 'yes' then 0 else 1 end,
                                 case when nullif(iata_code, '') is not null then 0 else 1 end,
                                 name,
                                 id
                        limit 1
                        """);
        if (airports.isEmpty()) {
            return new AirportRow(1L, "APT-DEMO", "APT-DEMO", "Airport Demo", "Airport Demo", "Portugal", "airport", null, null);
        }
        return airports.getFirst();
    }

    private String activeAirportCode() {
        String configuredDefault = blankToDefault(properties.getDefaultAirportCode(), null);
        if (configuredDefault != null) {
            try {
                return airportByCode(configuredDefault).code();
            } catch (IllegalArgumentException ignored) {
                // Fall back to the best imported airport below.
            }
        }
        return defaultAirport().code();
    }

    private boolean hasWeatherFor(String airportCode) {
        Long count = jdbcTemplate.queryForObject("""
                select count(*) from import_weather_snapshots where airport_id = ?
                """, Long.class, airportCode);
        return count != null && count > 0;
    }

    private AirportRow airportByCode(String airportCode) {
        List<AirportRow> airports = queryAirportRows("""
                        select id,
                               coalesce(nullif(iata_code, ''), nullif(ident, ''), 'APT-' || id::text) as code,
                               ident,
                               name,
                               coalesce(nullif(municipality, ''), name) as city,
                               coalesce(nullif(iso_country, ''), 'UNKNOWN') as country,
                               coalesce(nullif(type, ''), 'airport') as type,
                               latitude_deg,
                               longitude_deg
                        from import_airports
                        where coalesce(nullif(iata_code, ''), nullif(ident, '')) = ?
                           or iata_code = ?
                           or ident = ?
                        order by id
                        limit 1
                        """,
                airportCode,
                airportCode,
                airportCode);
        if (airports.isEmpty()) {
            throw new IllegalArgumentException("Unknown airport code: " + airportCode);
        }
        return airports.getFirst();
    }

    private List<AirportOption> queryAirportOptions(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AirportOption(
                rs.getString("code"),
                rs.getString("ident"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("type"),
                nullableDouble(rs, "latitude_deg"),
                nullableDouble(rs, "longitude_deg"),
                rs.getLong("runways")
        ), args);
    }

    private List<AirportRow> queryAirportRows(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AirportRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("ident"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("type"),
                nullableDouble(rs, "latitude_deg"),
                nullableDouble(rs, "longitude_deg")
        ), args);
    }

    private void insertWeatherSnapshot(String airportCode, WeatherInput input) {
        WeatherInput weather = normalizeWeatherInput(input);
        jdbcTemplate.update("""
                insert into import_weather_snapshots (
                    airport_id,
                    observed_at,
                    temperature_celsius,
                    feels_like_celsius,
                    wind_speed_kmh,
                    wind_gust_kmh,
                    wind_direction_degrees,
                    rain_mm_per_hour,
                    snow_mm_per_hour,
                    hail,
                    thunderstorm,
                    visibility_meters,
                    fog,
                    cloud_coverage_percent,
                    ceiling_meters,
                    cloud_label,
                    runway_surface,
                    weather_severity
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                airportCode,
                LocalDateTime.now(clock),
                weather.temperatureCelsius(),
                weather.feelsLikeCelsius(),
                weather.windSpeedKmh(),
                weather.windGustKmh(),
                weather.windDirectionDegrees(),
                weather.rainMmPerHour(),
                weather.snowMmPerHour(),
                weather.hail(),
                weather.thunderstorm(),
                weather.visibilityMeters(),
                weather.fog(),
                weather.cloudCoveragePercent(),
                weather.ceilingMeters(),
                weather.cloudLabel(),
                weather.runwaySurface(),
                weather.severityCode());
    }

    private WeatherInput normalizeWeatherInput(WeatherInput input) {
        int windDirection = Math.floorMod(input.windDirectionDegrees(), 360);
        int visibility = Math.max(0, input.visibilityMeters());
        int clouds = clamp(input.cloudCoveragePercent(), 0, 100);
        int ceiling = Math.max(0, input.ceilingMeters());
        String cloudLabel = blankToDefault(input.cloudLabel(), cloudLabel(input.thunderstorm(), input.fog(), clouds));
        String runwaySurface = blankToDefault(input.runwaySurface(), runwaySurface(input.rainMmPerHour(), input.snowMmPerHour(), input.hail()));
        String severityCode = WeatherSeverity.fromCode(input.severityCode()).max(severityFor(
                input.windSpeedKmh(),
                input.windGustKmh(),
                input.rainMmPerHour(),
                input.snowMmPerHour(),
                input.hail(),
                input.thunderstorm(),
                visibility,
                input.fog(),
                clouds
        )).code();
        return new WeatherInput(
                input.temperatureCelsius(),
                input.feelsLikeCelsius(),
                Math.max(0.0, input.windSpeedKmh()),
                Math.max(0.0, input.windGustKmh()),
                windDirection,
                Math.max(0.0, input.rainMmPerHour()),
                Math.max(0.0, input.snowMmPerHour()),
                input.hail(),
                input.thunderstorm(),
                visibility,
                input.fog(),
                clouds,
                ceiling,
                cloudLabel,
                runwaySurface,
                severityCode);
    }

    private JsonNode fetchCurrentWeather(double latitude, double longitude) {
        String uri = UriComponentsBuilder.fromPath("/v1/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,apparent_temperature,precipitation,rain,snowfall,weather_code,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility")
                .queryParam("wind_speed_unit", "kmh")
                .queryParam("precipitation_unit", "mm")
                .queryParam("timezone", "auto")
                .build()
                .toUriString();
        JsonNode response = weatherClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("current")) {
            throw new IllegalStateException("Weather API did not return current conditions");
        }
        return response.path("current");
    }

    private WeatherInput weatherInputFromApi(JsonNode current) {
        int weatherCode = current.path("weather_code").asInt(0);
        double rain = current.path("rain").asDouble(0.0);
        double precipitation = current.path("precipitation").asDouble(0.0);
        double snow = current.path("snowfall").asDouble(0.0);
        int clouds = current.path("cloud_cover").asInt(0);
        boolean thunderstorm = weatherCode >= 95;
        boolean fog = weatherCode == 45 || weatherCode == 48;
        boolean hail = weatherCode == 96 || weatherCode == 99;
        double rainAmount = Math.max(rain, precipitation - snow);
        String severity = severityFor(
                current.path("wind_speed_10m").asDouble(0.0),
                current.path("wind_gusts_10m").asDouble(0.0),
                rainAmount,
                snow,
                hail,
                thunderstorm,
                current.path("visibility").asInt(10000),
                fog,
                clouds
        ).code();
        return new WeatherInput(
                current.path("temperature_2m").asDouble(20.0),
                current.path("apparent_temperature").asDouble(current.path("temperature_2m").asDouble(20.0)),
                current.path("wind_speed_10m").asDouble(0.0),
                current.path("wind_gusts_10m").asDouble(0.0),
                current.path("wind_direction_10m").asInt(0),
                rainAmount,
                snow,
                hail,
                thunderstorm,
                current.path("visibility").asInt(10000),
                fog,
                clouds,
                ceilingFor(clouds, fog, thunderstorm),
                cloudLabel(thunderstorm, fog, clouds),
                runwaySurface(rainAmount, snow, hail),
                severity);
    }

    private WeatherSeverity severityFor(
            double windSpeedKmh,
            double windGustKmh,
            double rainMmPerHour,
            double snowMmPerHour,
            boolean hail,
            boolean thunderstorm,
            int visibilityMeters,
            boolean fog,
            int cloudCoveragePercent
    ) {
        if (hail || thunderstorm || windGustKmh >= 85.0 || visibilityMeters < 800 || snowMmPerHour >= 5.0) {
            return WeatherSeverity.GROUND_STOP;
        }
        if (windGustKmh >= 65.0 || windSpeedKmh >= 45.0 || visibilityMeters < 1600 || rainMmPerHour >= 8.0 || snowMmPerHour >= 1.5) {
            return WeatherSeverity.SEVERE;
        }
        if (fog || windGustKmh >= 40.0 || windSpeedKmh >= 28.0 || visibilityMeters < 5000 || rainMmPerHour > 0.0 || snowMmPerHour > 0.0 || cloudCoveragePercent >= 75) {
            return WeatherSeverity.CAUTION;
        }
        return WeatherSeverity.NORMAL;
    }

    private int ceilingFor(int clouds, boolean fog, boolean thunderstorm) {
        if (fog || thunderstorm) {
            return 400;
        }
        if (clouds >= 85) {
            return 700;
        }
        if (clouds >= 65) {
            return 1200;
        }
        if (clouds >= 35) {
            return 2500;
        }
        return 5000;
    }

    private String cloudLabel(boolean thunderstorm, boolean fog, int cloudCoveragePercent) {
        if (thunderstorm) {
            return "Thunderstorm";
        }
        if (fog) {
            return "Fog";
        }
        if (cloudCoveragePercent >= 85) {
            return "Overcast";
        }
        if (cloudCoveragePercent >= 65) {
            return "Broken Clouds";
        }
        if (cloudCoveragePercent >= 35) {
            return "Scattered Clouds";
        }
        if (cloudCoveragePercent > 0) {
            return "Few Clouds";
        }
        return "Clear";
    }

    private String runwaySurface(double rainMmPerHour, double snowMmPerHour, boolean hail) {
        if (hail || snowMmPerHour > 0.0) {
            return "CONTAMINATED";
        }
        if (rainMmPerHour > 0.0) {
            return "WET";
        }
        return "DRY";
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return value == null ? 0 : value;
    }

    private boolean isBootstrapped() {
        Long value = jdbcTemplate.queryForObject("""
                select count(*) from bootstrap_status where bootstrap_key = 'import_seeded' and bootstrap_value = 'true'
                """, Long.class);
        return value != null && value > 0;
    }

    private void markBootstrapped() {
        jdbcTemplate.update("""
                insert into bootstrap_status (bootstrap_key, bootstrap_value, updated_at)
                values ('import_seeded', 'true', ?)
                on conflict (bootstrap_key) do update set
                    bootstrap_value = excluded.bootstrap_value,
                    updated_at = excluded.updated_at
                """, LocalDateTime.now(clock));
    }

    private void truncateAllTables() {
        jdbcTemplate.execute("""
                truncate table
                    simulation_events,
                    simulation_flights,
                    simulation_state,
                    bootstrap_status,
                    import_weather_snapshots,
                    import_airline_flights,
                    import_navaids,
                    import_runways,
                    import_airports,
                    import_regions,
                    import_countries
                restart identity
                """);
    }

    private SimulationStateRow loadState() {
        List<SimulationStateRow> rows = jdbcTemplate.query("""
                        select lifecycle_state, simulated_time, multiplier, running, active_airport_code, updated_at
                        from simulation_state
                        where id = 1
                        """,
                (rs, rowNum) -> new SimulationStateRow(
                        rs.getString("lifecycle_state"),
                        rs.getObject("simulated_time", LocalDateTime.class),
                        rs.getString("multiplier"),
                        rs.getBoolean("running"),
                        rs.getString("active_airport_code"),
                        rs.getObject("updated_at", LocalDateTime.class)
                ));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private SimulationStateRow requireState() {
        SimulationStateRow state = loadState();
        if (state == null) {
            resetSimulationState();
            state = loadState();
        }
        if (state == null) {
            throw new IllegalStateException("Simulation state could not be initialized");
        }
        return state;
    }

    private void insertEvent(String level, String category, String message) {
        jdbcTemplate.update("""
                insert into simulation_events (occurred_at, level, category, message)
                values (?, ?, ?, ?)
                """,
                LocalDateTime.now(clock),
                level,
                category,
                message);
    }

    private String gateFor(int index) {
        return "A" + ((index % 6) + 1);
    }

    private String runwayFor(int index) {
        return "RWY-" + ((index % 2) + 1);
    }

    private String weatherMessage(WeatherSeverity severity) {
        return switch (severity.code()) {
            case "CAUTION" -> "Reduced visibility may slow departures";
            case "SEVERE" -> "Departures may be delayed";
            case "GROUND_STOP" -> "Operations are on hold";
            default -> "Conditions normal";
        };
    }

    private Path path(String name) {
        return properties.importDirectory().resolve(name);
    }

    private static String normalizeRequiredCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Airport code is required");
        }
        return value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Double nullableDouble(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private record SimulationStateRow(
            String lifecycleState,
            LocalDateTime simulatedTime,
            String multiplier,
            boolean running,
            String activeAirportCode,
            LocalDateTime updatedAt
    ) {
    }

    private record AirportRow(
            long id,
            String code,
            String ident,
            String name,
            String city,
            String country,
            String type,
            Double latitude,
            Double longitude
    ) {
    }

    private record WeatherRow(
            String airportId,
            LocalDateTime observedAt,
            double temperatureCelsius,
            double feelsLikeCelsius,
            double windSpeedKmh,
            double windGustKmh,
            int windDirectionDegrees,
            double rainMmPerHour,
            double snowMmPerHour,
            boolean hail,
            boolean thunderstorm,
            int visibilityMeters,
            boolean fog,
            int cloudCoveragePercent,
            int ceilingMeters,
            String cloudLabel,
            String runwaySurface,
            String severityCode
    ) {
    }

    private record FlightRow(
            long id,
            long sourceRowId,
            String flightNumber,
            String airline,
            String originLabel,
            String destinationLabel,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            String status,
            int delayMinutes,
            String gate,
            String runway,
            String weatherNotes
    ) {
    }

    private record TemplateFlightRow(
            long rowId,
            String airline,
            String flightNumber,
            String sourceCity,
            String destinationCity,
            double durationHours,
            int daysLeft
    ) {
    }

    private record EventRow(LocalDateTime occurredAt, String level, String category, String message) {
    }
}
