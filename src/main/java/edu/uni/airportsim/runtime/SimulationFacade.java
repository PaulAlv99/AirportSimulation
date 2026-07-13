package edu.uni.airportsim.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import edu.uni.airportsim.config.AirportSimulationProperties;
import edu.uni.airportsim.data.CsvCopyLoader;
import edu.uni.airportsim.io.CsvParser;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class SimulationFacade implements ApplicationRunner {
    private static final String STATE_ID = "1";
    private static final int MAX_DETAIL_ROWS = 250;
    private static final Set<String> FLIGHT_CONTROL_STATUSES = Set.of(
            "SCHEDULED",
            "CHECK_IN_OPEN",
            "BOARDING",
            "DELAYED",
            "DEPARTED",
            "ARRIVED",
            "CANCELLED"
    );

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
        boolean wasRunning = requireState().running();
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
        resetSimulationState(airport.code());
        if (wasRunning) {
            start();
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
        updateOperationState(nextTime, severity);
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
                count("import_openflights_airlines"),
                count("import_openflights_routes"),
                count("import_openflights_planes"),
                count("simulation_flights"),
                count("simulation_passengers"),
                count("simulation_baggage"),
                count("simulation_gates"),
                count("simulation_ground_operations"),
                count("simulation_events")
        );
        OperationSummary operations = operationSummary();
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
                operations,
                flights,
                events
        );
    }

    public OperationSummary operationSummary() {
        long totalFlights = count("simulation_flights");
        long activeFlights = countFlightsWhere("status not in ('ARRIVED', 'CANCELLED')");
        long delayedFlights = countFlightsWhere("status = 'DELAYED'");
        long cancelledFlights = countFlightsWhere("status = 'CANCELLED'");
        long boardingFlights = countFlightsWhere("status = 'BOARDING'");
        long departedFlights = countFlightsWhere("status = 'DEPARTED'");
        long arrivedFlights = countFlightsWhere("status = 'ARRIVED'");
        long passengersTotal = count("simulation_passengers");
        long passengersCheckedIn = countPassengersWhere("checked_in = true");
        long passengersSecurityCleared = countPassengersWhere("security_cleared = true");
        long passengersBoarded = countPassengersWhere("boarded = true");
        long passengersMissedConnections = countPassengersWhere("missed_connection = true");
        long baggageTotal = count("simulation_baggage");
        long bagsRegistered = countBaggageWhere("status = 'REGISTERED'");
        long bagsScreened = countBaggageWhere("status = 'SCREENED'");
        long bagsLoaded = countBaggageWhere("status = 'LOADED'");
        long bagsInTransit = countBaggageWhere("status = 'IN_TRANSIT'");
        long bagsDelivered = countBaggageWhere("status = 'DELIVERED'");
        long bagsDelayed = countBaggageWhere("status = 'DELAYED'");
        long bagsLost = countBaggageWhere("status = 'LOST'");
        long gatesTotal = count("simulation_gates");
        long gatesOpen = countGatesWhere("open = true");
        long gatesOccupied = countGatesWhere("state in ('OCCUPIED', 'BOARDING', 'TURNAROUND', 'DELAYED')");
        long gateUtilizationPercent = gatesOpen == 0 ? 0 : Math.round((gatesOccupied * 100.0) / gatesOpen);
        long runwayQueue = countFlightsWhere("status in ('BOARDING', 'DELAYED')");
        long checkInQueue = countPassengersWhere("checked_in = false and status <> 'MISSED_CONNECTION'");
        long securityQueue = countPassengersWhere("checked_in = true and security_cleared = false and status <> 'MISSED_CONNECTION'");
        long baggageBacklog = countBaggageWhere("status in ('REGISTERED', 'SCREENED', 'DELAYED')");
        long activeGroundOps = countGroundWhere("status = 'ACTIVE'");
        long delayedGroundOps = countGroundWhere("status = 'DELAYED'");
        return new OperationSummary(
                safeTrafficProfile(),
                targetFlightCount(),
                safeLoadFactor(),
                safeBagRate(),
                totalFlights,
                activeFlights,
                delayedFlights,
                cancelledFlights,
                boardingFlights,
                departedFlights,
                arrivedFlights,
                passengersTotal,
                passengersCheckedIn,
                passengersSecurityCleared,
                passengersBoarded,
                passengersMissedConnections,
                baggageTotal,
                bagsRegistered,
                bagsScreened,
                bagsLoaded,
                bagsInTransit,
                bagsDelivered,
                bagsDelayed,
                bagsLost,
                gatesTotal,
                gatesOpen,
                gatesOccupied,
                gateUtilizationPercent,
                runwayQueue,
                checkInQueue,
                securityQueue,
                baggageBacklog,
                activeGroundOps,
                delayedGroundOps
        );
    }

    public List<BaggageView> baggage(String status, String flight, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_DETAIL_ROWS));
        String normalizedStatus = blankToDefault(status, null);
        String normalizedFlight = blankToDefault(flight, null);
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                select b.id,
                       b.tag,
                       b.flight_id,
                       f.flight_number,
                       b.status,
                       b.belt,
                       b.exception_reason,
                       b.last_updated
                from simulation_baggage b
                join simulation_flights f on f.id = b.flight_id
                where 1 = 1
                """);
        if (normalizedStatus != null) {
            sql.append(" and b.status = ?");
            args.add(normalizedStatus.toUpperCase(Locale.ROOT));
        }
        if (normalizedFlight != null) {
            sql.append(" and (f.flight_number ilike ? or f.id::text = ?)");
            args.add("%" + normalizedFlight + "%");
            args.add(normalizedFlight);
        }
        sql.append(" order by b.last_updated desc, b.id desc limit ?");
        args.add(safeLimit);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new BaggageView(
                rs.getLong("id"),
                rs.getString("tag"),
                rs.getLong("flight_id"),
                rs.getString("flight_number"),
                rs.getString("status"),
                rs.getString("belt"),
                rs.getString("exception_reason"),
                rs.getObject("last_updated", LocalDateTime.class)
        ), args.toArray());
    }

    public List<GateView> gates() {
        return jdbcTemplate.query("""
                select g.id,
                       g.gate_code,
                       g.terminal,
                       g.state,
                       g.open,
                       g.flight_id,
                       f.flight_number,
                       g.passenger_queue,
                       g.baggage_queue,
                       g.last_updated
                from simulation_gates g
                left join simulation_flights f on f.id = g.flight_id
                order by g.gate_code
                """, (rs, rowNum) -> new GateView(
                rs.getLong("id"),
                rs.getString("gate_code"),
                rs.getString("terminal"),
                rs.getString("state"),
                rs.getBoolean("open"),
                nullableLong(rs, "flight_id"),
                rs.getString("flight_number"),
                rs.getInt("passenger_queue"),
                rs.getInt("baggage_queue"),
                rs.getObject("last_updated", LocalDateTime.class)
        ));
    }

    public List<GroundOperationView> groundOperations() {
        return jdbcTemplate.query("""
                select o.id,
                       o.flight_id,
                       f.flight_number,
                       o.gate_code,
                       o.operation_type,
                       o.status,
                       o.started_at,
                       o.due_at,
                       o.completed_at,
                       o.delay_minutes
                from simulation_ground_operations o
                left join simulation_flights f on f.id = o.flight_id
                order by case o.status
                             when 'DELAYED' then 0
                             when 'ACTIVE' then 1
                             when 'PENDING' then 2
                             else 3
                         end,
                         o.due_at nulls last,
                         o.id
                limit 160
                """, (rs, rowNum) -> new GroundOperationView(
                rs.getLong("id"),
                nullableLong(rs, "flight_id"),
                rs.getString("flight_number"),
                rs.getString("gate_code"),
                rs.getString("operation_type"),
                rs.getString("status"),
                rs.getObject("started_at", LocalDateTime.class),
                rs.getObject("due_at", LocalDateTime.class),
                rs.getObject("completed_at", LocalDateTime.class),
                rs.getInt("delay_minutes")
        ));
    }

    @Transactional
    public void controlFlight(long flightId, String status, Integer delayMinutes, String reason) {
        FlightRow flight = requireFlight(flightId);
        String normalizedStatus = blankToDefault(status, flight.status()).toUpperCase(Locale.ROOT);
        if (!FLIGHT_CONTROL_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported flight status: " + normalizedStatus);
        }
        int safeDelay = delayMinutes == null ? flight.delayMinutes() : clamp(delayMinutes, 0, 720);
        String delayReason = blankToDefault(reason, normalizedStatus.equals("DELAYED") ? "Manual operations control" : flight.delayReason());
        jdbcTemplate.update("""
                update simulation_flights
                set status = ?,
                    delay_minutes = ?,
                    delay_reason = ?,
                    weather_notes = case when ? = 'DELAYED' then ? else weather_notes end,
                    last_updated = ?
                where id = ?
                """,
                normalizedStatus,
                safeDelay,
                delayReason,
                normalizedStatus,
                delayReason,
                LocalDateTime.now(clock),
                flightId);
        if ("CANCELLED".equals(normalizedStatus)) {
            jdbcTemplate.update("""
                    update simulation_passengers
                    set status = 'MISSED_CONNECTION',
                        missed_connection = true,
                        last_updated = ?
                    where flight_id = ?
                    """, LocalDateTime.now(clock), flightId);
            jdbcTemplate.update("""
                    update simulation_baggage
                    set status = 'DELAYED',
                        exception_reason = ?,
                        last_updated = ?
                    where flight_id = ? and status not in ('DELIVERED', 'LOST')
                    """, "Flight cancelled", LocalDateTime.now(clock), flightId);
        }
        refreshResourceQueues(requireState().simulatedTime());
        insertEvent("WARN", "FLIGHT", flight.flightNumber() + " forced to " + normalizedStatus + " by operations control");
    }

    @Transactional
    public void applyDisruption(String type, int severity, int durationMinutes, String gateCode) {
        String normalizedType = blankToDefault(type, "BAGGAGE_JAM").toUpperCase(Locale.ROOT);
        int safeSeverity = clamp(severity, 1, 5);
        int safeDuration = clamp(durationMinutes, 5, 240);
        switch (normalizedType) {
            case "BAGGAGE_JAM" -> applyBaggageJam(safeSeverity, safeDuration);
            case "STAFFING" -> applyStaffingDisruption(safeSeverity, safeDuration);
            case "GATE_CLOSE" -> applyGateClose(gateCode, safeSeverity, safeDuration);
            case "GATE_OPEN" -> applyGateOpen(gateCode);
            case "RUNWAY_HOLD" -> applyRunwayHold(safeSeverity, safeDuration);
            default -> throw new IllegalArgumentException("Unsupported disruption type: " + normalizedType);
        }
        refreshResourceQueues(requireState().simulatedTime());
        insertEvent("WARN", "OPERATIONS", normalizedType + " disruption applied for " + safeDuration + " minutes");
    }

    private void ensureRuntimeState() {
        if (properties.isUseOpenFlights() && count("import_openflights_routes") == 0) {
            importOpenFlightsData();
            markBootstrapped();
        }
        if (count("import_airports") == 0 || count("import_airline_flights") == 0) {
            importAllData();
            markBootstrapped();
        }
        if (loadState() == null || count("simulation_flights") == 0 || count("simulation_baggage") == 0 || count("simulation_gates") == 0) {
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
        if (properties.isUseOpenFlights()) {
            importOpenFlightsData();
        }

        if (count("import_weather_snapshots") == 0) {
            seedSyntheticWeather(activeAirportCode());
        }
    }

    private void importOpenFlightsData() {
        Path openFlightsDir = path("openflights");
        importOpenFlightsAirlines(openFlightsDir.resolve("airlines.dat"));
        importOpenFlightsPlanes(openFlightsDir.resolve("planes.dat"));
        importOpenFlightsRoutes(openFlightsDir.resolve("routes.dat"));
    }

    private void importOpenFlightsAirlines(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        jdbcTemplate.update("delete from import_openflights_airlines");
        List<Object[]> rows = new ArrayList<>();
        for (List<String> values : readDatRows(file)) {
            if (values.size() < 8) {
                continue;
            }
            Long airlineId = nullableLong(values.get(0));
            if (airlineId == null) {
                continue;
            }
            rows.add(new Object[] {
                    airlineId,
                    nullToDefault(openFlightsValue(values.get(1)), "Unknown Airline"),
                    openFlightsValue(values.get(2)),
                    openFlightsValue(values.get(3)),
                    openFlightsValue(values.get(4)),
                    openFlightsValue(values.get(5)),
                    openFlightsValue(values.get(6)),
                    openFlightsValue(values.get(7))
            });
        }
        batchUpdate("""
                insert into import_openflights_airlines (
                    airline_id,
                    name,
                    alias,
                    iata_code,
                    icao_code,
                    callsign,
                    country,
                    active
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (airline_id) do update set
                    name = excluded.name,
                    alias = excluded.alias,
                    iata_code = excluded.iata_code,
                    icao_code = excluded.icao_code,
                    callsign = excluded.callsign,
                    country = excluded.country,
                    active = excluded.active
                """, rows);
    }

    private void importOpenFlightsPlanes(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        jdbcTemplate.update("delete from import_openflights_planes");
        List<Object[]> rows = new ArrayList<>();
        for (List<String> values : readDatRows(file)) {
            if (values.size() < 3) {
                continue;
            }
            rows.add(new Object[] {
                    nullToDefault(openFlightsValue(values.get(0)), "Unknown Aircraft"),
                    openFlightsValue(values.get(1)),
                    openFlightsValue(values.get(2))
            });
        }
        batchUpdate("""
                insert into import_openflights_planes (
                    name,
                    iata_code,
                    icao_code
                ) values (?, ?, ?)
                """, rows);
    }

    private void importOpenFlightsRoutes(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        jdbcTemplate.update("delete from import_openflights_routes");
        List<Object[]> rows = new ArrayList<>();
        for (List<String> values : readDatRows(file)) {
            if (values.size() < 9) {
                continue;
            }
            String sourceAirport = openFlightsValue(values.get(2));
            String destinationAirport = openFlightsValue(values.get(4));
            if (sourceAirport == null || destinationAirport == null) {
                continue;
            }
            rows.add(new Object[] {
                    openFlightsValue(values.get(0)),
                    openFlightsValue(values.get(1)),
                    sourceAirport,
                    openFlightsValue(values.get(3)),
                    destinationAirport,
                    openFlightsValue(values.get(5)),
                    openFlightsValue(values.get(6)),
                    nullableInteger(values.get(7), 0),
                    openFlightsValue(values.get(8))
            });
        }
        batchUpdate("""
                insert into import_openflights_routes (
                    airline_code,
                    airline_id,
                    source_airport,
                    source_airport_id,
                    destination_airport,
                    destination_airport_id,
                    codeshare,
                    stops,
                    equipment
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);
    }

    private void resetSimulationState() {
        SimulationStateRow currentState = loadState();
        String selectedAirport = currentState == null ? activeAirportCode() : blankToDefault(currentState.activeAirportCode(), activeAirportCode());
        resetSimulationState(selectedAirport);
    }

    private void resetSimulationState(String selectedAirportCode) {
        jdbcTemplate.update("""
                delete from simulation_baggage
                """);
        jdbcTemplate.update("""
                delete from simulation_passengers
                """);
        jdbcTemplate.update("""
                delete from simulation_ground_operations
                """);
        jdbcTemplate.update("""
                delete from simulation_gates
                """);
        jdbcTemplate.update("""
                delete from simulation_resource_queues
                """);
        jdbcTemplate.update("""
                delete from simulation_flights
                """);
        jdbcTemplate.update("""
                delete from simulation_events
                """);

        String activeAirportCode = airportByCode(selectedAirportCode).code();
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

        seedDemoFlights(simulatedTime, activeAirportCode);
        if (!hasWeatherFor(activeAirportCode)) {
            seedSyntheticWeather(activeAirportCode);
        }
        updateOperationState(simulatedTime, WeatherSeverity.fromCode(currentWeather(activeAirportCode).severityCode()));
        insertEvent("INFO", "SIMULATION", "Simulation reset");
    }

    private void seedDemoFlights(LocalDateTime baseTime, String activeAirportCode) {
        int targetFlights = targetFlightCount();
        List<RouteTemplateRow> sourceRows = loadRouteTemplateRows(activeAirportCode, Math.max(targetFlights, properties.getFlightSeedLimit()));
        if (sourceRows.isEmpty()) {
            sourceRows = legacyRouteTemplates(activeAirportCode, Math.max(targetFlights, properties.getFlightSeedLimit()));
        }
        if (sourceRows.isEmpty()) {
            sourceRows = fallbackRouteTemplateRows(activeAirportCode);
        }

        List<Object[]> rows = new ArrayList<>();
        int spacingMinutes = Math.max(3, Math.min(12, 1440 / Math.max(1, targetFlights)));
        WeatherSeverity weatherSeverity = WeatherSeverity.fromCode(currentWeather(activeAirportCode).severityCode());
        for (int index = 0; index < targetFlights; index++) {
            RouteTemplateRow template = sourceRows.get(index % sourceRows.size());
            LocalDateTime departure = baseTime.minusMinutes(150).plusMinutes(index * (long) spacingMinutes + (index % 5L));
            long durationMinutes = Math.max(35L, Math.round(durationHoursFor(template.sourceAirport(), template.destinationAirport(), template.aircraftCode(), index) * 60.0));
            LocalDateTime arrival = departure.plusMinutes(durationMinutes);
            int delayMinutes = initialDelayMinutes(weatherSeverity, index);
            int passengerCount = passengerCountFor(template.aircraftCode(), index);
            int baggageCount = baggageCountFor(passengerCount, index);
            String status = statusFor(departure, arrival, delayMinutes, baseTime);
            if (delayMinutes > 0 && "BOARDING".equals(status)) {
                status = "DELAYED";
            }
            rows.add(new Object[] {
                    template.rowId(),
                    flightNumberFor(template, index),
                    template.airlineName(),
                    airportLabel(template.sourceAirport()),
                    airportLabel(template.destinationAirport()),
                    departure,
                    arrival,
                    status,
                    delayMinutes,
                    gateFor(index, gateCountFor(targetFlights)),
                    runwayFor(index),
                    delayMinutes > 0 ? "Weather delay during seed" : null,
                    template.routeSource(),
                    template.aircraftCode(),
                    template.aircraftName(),
                    passengerCount,
                    baggageCount,
                    directionFor(template, activeAirportCode),
                    delayMinutes > 0 ? "Weather delay during seed" : null,
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
                    route_source,
                    aircraft_code,
                    aircraft_name,
                    passenger_count,
                    baggage_count,
                    direction,
                    delay_reason,
                    last_updated
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, rows);
        seedGeneratedOperations(baseTime, targetFlights);
    }

    private List<RouteTemplateRow> loadRouteTemplateRows(String activeAirportCode, int limit) {
        if (!properties.isUseOpenFlights() || count("import_openflights_routes") == 0) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return jdbcTemplate.query("""
                select r.id,
                       coalesce(nullif(r.airline_code, ''), nullif(a.iata_code, ''), nullif(a.icao_code, ''), 'SIM') as airline_code,
                       coalesce(nullif(a.name, ''), nullif(r.airline_code, ''), 'Simulation Airline') as airline_name,
                       r.source_airport,
                       r.destination_airport,
                       coalesce(nullif(split_part(coalesce(r.equipment, ''), ' ', 1), ''), '320') as aircraft_code,
                       coalesce(nullif(p.name, ''), 'Passenger Aircraft') as aircraft_name,
                       'OPENFLIGHTS' as route_source
                from import_openflights_routes r
                left join import_openflights_airlines a
                    on a.airline_id::text = r.airline_id
                    or a.iata_code = r.airline_code
                    or a.icao_code = r.airline_code
                left join import_openflights_planes p
                    on p.iata_code = split_part(coalesce(r.equipment, ''), ' ', 1)
                    or p.icao_code = split_part(coalesce(r.equipment, ''), ' ', 1)
                where (r.source_airport = ? or r.destination_airport = ?)
                  and r.stops = 0
                order by case when coalesce(r.codeshare, '') = 'Y' then 1 else 0 end,
                         r.id
                limit ?
                """,
                (rs, rowNum) -> new RouteTemplateRow(
                        rs.getLong("id"),
                        rs.getString("airline_code"),
                        rs.getString("airline_name"),
                        rs.getString("source_airport"),
                        rs.getString("destination_airport"),
                        rs.getString("aircraft_code"),
                        rs.getString("aircraft_name"),
                        rs.getString("route_source")
                ),
                activeAirportCode,
                activeAirportCode,
                safeLimit);
    }

    private List<RouteTemplateRow> legacyRouteTemplates(String activeAirportCode, int limit) {
        List<TemplateFlightRow> legacyRows = loadTemplateFlights(limit);
        List<RouteTemplateRow> rows = new ArrayList<>();
        for (int index = 0; index < legacyRows.size(); index++) {
            TemplateFlightRow row = legacyRows.get(index);
            String destination = codeLike(row.destinationCity(), "DST" + ((index % 90) + 10));
            String source = codeLike(row.sourceCity(), "SRC" + ((index % 90) + 10));
            boolean outbound = index % 2 == 0;
            rows.add(new RouteTemplateRow(
                    row.rowId(),
                    airlineCodeFromFlight(row.flightNumber()),
                    row.airline(),
                    outbound ? activeAirportCode : source,
                    outbound ? destination : activeAirportCode,
                    "320",
                    "Passenger Aircraft",
                    "CSV_FALLBACK"
            ));
        }
        return rows;
    }

    private List<RouteTemplateRow> fallbackRouteTemplateRows(String airportCode) {
        return List.of(
                new RouteTemplateRow(1L, "SIM", "Airport Simulation", airportCode, "OPO", "320", "Airbus A320", "SIMULATED"),
                new RouteTemplateRow(2L, "SIM", "Airport Simulation", "OPO", airportCode, "320", "Airbus A320", "SIMULATED"),
                new RouteTemplateRow(3L, "SIM", "Airport Simulation", airportCode, "FNC", "AT7", "ATR 72", "SIMULATED")
        );
    }

    private void seedGeneratedOperations(LocalDateTime baseTime, int targetFlights) {
        seedGates(targetFlights);
        List<FlightRow> flights = loadFlights();
        List<Object[]> passengers = new ArrayList<>();
        List<Object[]> baggage = new ArrayList<>();
        List<Object[]> groundOps = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        for (FlightRow flight : flights) {
            PassengerState passengerState = passengerStateFor(flight.status());
            BaggageStateRow baggageState = baggageStateFor(flight.status());
            for (int index = 0; index < flight.passengerCount(); index++) {
                boolean hasBag = index < flight.baggageCount();
                passengers.add(new Object[] {
                        flight.id(),
                        passengerCode(flight.id(), index),
                        passengerState.status(),
                        passengerState.checkedIn(),
                        passengerState.securityCleared(),
                        passengerState.boarded(),
                        passengerState.missedConnection(),
                        hasBag ? 1 : 0,
                        now
                });
            }
            for (int index = 0; index < flight.baggageCount(); index++) {
                baggage.add(new Object[] {
                        flight.id(),
                        null,
                        baggageTag(flight.id(), index),
                        baggageState.status(),
                        baggageBeltFor(flight.id()),
                        baggageState.exceptionReason(),
                        now
                });
            }
            addGroundOperations(groundOps, flight, now);
        }
        batchUpdate("""
                insert into simulation_passengers (
                    flight_id,
                    passenger_code,
                    status,
                    checked_in,
                    security_cleared,
                    boarded,
                    missed_connection,
                    baggage_count,
                    last_updated
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, passengers);
        batchUpdate("""
                insert into simulation_baggage (
                    flight_id,
                    passenger_id,
                    tag,
                    status,
                    belt,
                    exception_reason,
                    last_updated
                ) values (?, ?, ?, ?, ?, ?, ?)
                """, baggage);
        batchUpdate("""
                insert into simulation_ground_operations (
                    flight_id,
                    gate_code,
                    operation_type,
                    status,
                    started_at,
                    due_at,
                    completed_at,
                    delay_minutes,
                    last_updated
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, groundOps);
        updateOperationState(baseTime, WeatherSeverity.NORMAL);
    }

    private void seedGates(int targetFlights) {
        int gateCount = gateCountFor(targetFlights);
        List<Object[]> rows = new ArrayList<>();
        for (int index = 0; index < gateCount; index++) {
            rows.add(new Object[] {
                    gateFor(index, gateCount),
                    terminalFor(index),
                    "AVAILABLE",
                    true,
                    LocalDateTime.now(clock)
            });
        }
        batchUpdate("""
                insert into simulation_gates (
                    gate_code,
                    terminal,
                    state,
                    open,
                    last_updated
                ) values (?, ?, ?, ?, ?)
                """, rows);
    }

    private void addGroundOperations(List<Object[]> rows, FlightRow flight, LocalDateTime now) {
        addGroundOperation(rows, flight, "CHECK_IN", flight.departureTime().minusMinutes(120), flight.departureTime().minusMinutes(45), now);
        addGroundOperation(rows, flight, "SECURITY", flight.departureTime().minusMinutes(100), flight.departureTime().minusMinutes(25), now);
        addGroundOperation(rows, flight, "CLEANING", flight.departureTime().minusMinutes(70), flight.departureTime().minusMinutes(35), now);
        addGroundOperation(rows, flight, "FUELING", flight.departureTime().minusMinutes(65), flight.departureTime().minusMinutes(25), now);
        addGroundOperation(rows, flight, "CATERING", flight.departureTime().minusMinutes(55), flight.departureTime().minusMinutes(20), now);
        addGroundOperation(rows, flight, "BAGGAGE_LOADING", flight.departureTime().minusMinutes(50), flight.departureTime().minusMinutes(10), now);
        addGroundOperation(rows, flight, "BOARDING", flight.departureTime().minusMinutes(35), flight.departureTime(), now);
    }

    private void addGroundOperation(List<Object[]> rows, FlightRow flight, String operationType, LocalDateTime startedAt, LocalDateTime dueAt, LocalDateTime now) {
        String status;
        LocalDateTime completedAt = null;
        if ("CANCELLED".equals(flight.status())) {
            status = "CANCELLED";
        } else if (now.isBefore(startedAt)) {
            status = "PENDING";
        } else if (!now.isBefore(dueAt)) {
            status = "COMPLETED";
            completedAt = now;
        } else {
            status = "ACTIVE";
        }
        rows.add(new Object[] {
                flight.id(),
                flight.gate(),
                operationType,
                status,
                startedAt,
                dueAt,
                completedAt,
                0,
                now
        });
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
        if ("CANCELLED".equals(flight.status())) {
            return;
        }
        String effectiveStatus = statusFor(flight, currentTime);
        int delayMinutes = flight.delayMinutes();

        if (delayMinutes == 0
                && weatherSeverity.rank() >= WeatherSeverity.CAUTION.rank()
                && "BOARDING".equals(effectiveStatus)) {
            delayMinutes = weatherSeverity.rank() >= WeatherSeverity.SEVERE.rank() ? 20 : 10;
            effectiveStatus = "DELAYED";
            jdbcTemplate.update("""
                    update simulation_flights
                    set delay_minutes = ?, status = ?, weather_notes = ?, delay_reason = ?, last_updated = ?
                    where id = ?
                    """,
                    delayMinutes,
                    effectiveStatus,
                    "Weather delay due to " + weatherSeverity.label().toLowerCase(Locale.ROOT),
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
        if ("CANCELLED".equals(flight.status())) {
            return "CANCELLED";
        }
        return statusFor(flight.departureTime(), flight.arrivalTime(), flight.delayMinutes(), currentTime);
    }

    private String statusFor(LocalDateTime departureTime, LocalDateTime arrivalTime, int delayMinutes, LocalDateTime currentTime) {
        LocalDateTime departure = departureTime.plusMinutes(delayMinutes);
        LocalDateTime arrival = arrivalTime.plusMinutes(delayMinutes);
        if (currentTime.isBefore(departure.minusMinutes(30))) {
            return "SCHEDULED";
        }
        if (currentTime.isBefore(departure.minusMinutes(15))) {
            return "CHECK_IN_OPEN";
        }
        if (currentTime.isBefore(departure)) {
            return delayMinutes > 0 ? "DELAYED" : "BOARDING";
        }
        if (currentTime.isBefore(arrival)) {
            return "DEPARTED";
        }
        return "ARRIVED";
    }

    private void updateOperationState(LocalDateTime currentTime, WeatherSeverity weatherSeverity) {
        List<FlightRow> flights = loadFlights();
        for (FlightRow flight : flights) {
            PassengerState passengerState = passengerStateFor(flight.status());
            BaggageStateRow baggageState = baggageStateFor(flight.status());
            jdbcTemplate.update("""
                    update simulation_passengers
                    set status = ?,
                        checked_in = ?,
                        security_cleared = ?,
                        boarded = ?,
                        missed_connection = ?,
                        last_updated = ?
                    where flight_id = ?
                      and missed_connection = false
                    """,
                    passengerState.status(),
                    passengerState.checkedIn(),
                    passengerState.securityCleared(),
                    passengerState.boarded(),
                    passengerState.missedConnection(),
                    LocalDateTime.now(clock),
                    flight.id());
            if ("DELAYED".equals(flight.status())) {
                jdbcTemplate.update("""
                        update simulation_baggage
                        set status = 'DELAYED',
                            exception_reason = coalesce(exception_reason, ?),
                            last_updated = ?
                        where flight_id = ?
                          and status in ('REGISTERED', 'SCREENED', 'LOADED')
                          and id in (
                              select id
                              from simulation_baggage
                              where flight_id = ?
                                and status in ('REGISTERED', 'SCREENED', 'LOADED')
                              order by id
                              limit ?
                          )
                        """,
                        blankToDefault(flight.delayReason(), "Flight delay"),
                        LocalDateTime.now(clock),
                        flight.id(),
                        flight.id(),
                        Math.max(1, flight.baggageCount() / 8));
            } else {
                jdbcTemplate.update("""
                        update simulation_baggage
                        set status = ?,
                            exception_reason = ?,
                            last_updated = ?
                        where flight_id = ?
                          and status not in ('LOST', 'DELAYED')
                        """,
                        baggageState.status(),
                        baggageState.exceptionReason(),
                        LocalDateTime.now(clock),
                        flight.id());
            }
        }
        updateGroundOperations(currentTime, weatherSeverity);
        updateGates(currentTime);
        refreshResourceQueues(currentTime);
    }

    private void updateGroundOperations(LocalDateTime currentTime, WeatherSeverity weatherSeverity) {
        String activeStatus = weatherSeverity.rank() >= WeatherSeverity.SEVERE.rank() ? "DELAYED" : "ACTIVE";
        int weatherDelay = weatherSeverity.rank() >= WeatherSeverity.GROUND_STOP.rank() ? 30 : weatherSeverity.rank() >= WeatherSeverity.SEVERE.rank() ? 15 : 0;
        jdbcTemplate.update("""
                update simulation_ground_operations
                set status = case
                        when status = 'CANCELLED' then 'CANCELLED'
                        when ? < started_at then 'PENDING'
                        when ? >= due_at then 'COMPLETED'
                        when ? >= started_at then ?
                        else 'PENDING'
                    end,
                    completed_at = case
                        when ? >= due_at and completed_at is null then ?
                        else completed_at
                    end,
                    delay_minutes = case
                        when ? > 0 and ? >= started_at and ? < due_at then greatest(delay_minutes, ?)
                        else delay_minutes
                    end,
                    last_updated = ?
                """,
                currentTime,
                currentTime,
                currentTime,
                activeStatus,
                currentTime,
                currentTime,
                weatherDelay,
                currentTime,
                currentTime,
                weatherDelay,
                LocalDateTime.now(clock));
    }

    private void updateGates(LocalDateTime currentTime) {
        jdbcTemplate.update("""
                update simulation_gates
                set state = case when open then 'AVAILABLE' else 'CLOSED' end,
                    flight_id = null,
                    passenger_queue = 0,
                    baggage_queue = 0,
                    last_updated = ?
                """, LocalDateTime.now(clock));
        for (FlightRow flight : loadFlights()) {
            if (flight.gate() == null || !gateNeedsFlight(flight.status(), flight.departureTime(), flight.arrivalTime(), currentTime)) {
                continue;
            }
            int passengerQueue = jdbcTemplate.queryForObject("""
                    select count(*)
                    from simulation_passengers
                    where flight_id = ?
                      and boarded = false
                      and missed_connection = false
                    """, Integer.class, flight.id());
            int baggageQueue = jdbcTemplate.queryForObject("""
                    select count(*)
                    from simulation_baggage
                    where flight_id = ?
                      and status in ('REGISTERED', 'SCREENED', 'DELAYED')
                    """, Integer.class, flight.id());
            jdbcTemplate.update("""
                    update simulation_gates
                    set state = ?,
                        flight_id = ?,
                        passenger_queue = ?,
                        baggage_queue = ?,
                        last_updated = ?
                    where gate_code = ?
                      and open = true
                    """,
                    gateStateFor(flight.status()),
                    flight.id(),
                    passengerQueue,
                    baggageQueue,
                    LocalDateTime.now(clock),
                    flight.gate());
        }
    }

    private boolean gateNeedsFlight(String status, LocalDateTime departureTime, LocalDateTime arrivalTime, LocalDateTime currentTime) {
        if (Set.of("CHECK_IN_OPEN", "BOARDING", "DELAYED").contains(status)) {
            return true;
        }
        return currentTime.isAfter(departureTime.minusMinutes(90)) && currentTime.isBefore(arrivalTime.plusMinutes(20));
    }

    private String gateStateFor(String flightStatus) {
        return switch (flightStatus) {
            case "BOARDING" -> "BOARDING";
            case "DELAYED" -> "DELAYED";
            case "DEPARTED", "ARRIVED" -> "TURNAROUND";
            default -> "OCCUPIED";
        };
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
                       departure_time, arrival_time, status, delay_minutes, gate, runway, weather_notes,
                       route_source, aircraft_code, aircraft_name, passenger_count, baggage_count, direction, delay_reason
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
                        rs.getString("weather_notes"),
                        rs.getString("route_source"),
                        rs.getString("aircraft_code"),
                        rs.getString("aircraft_name"),
                        rs.getInt("passenger_count"),
                        rs.getInt("baggage_count"),
                        rs.getString("direction"),
                        rs.getString("delay_reason")
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
                row.weatherNotes(),
                row.routeSource(),
                row.aircraftCode(),
                row.aircraftName(),
                row.passengerCount(),
                row.baggageCount(),
                row.direction(),
                row.delayReason()
        );
    }

    private EventView toEventView(EventRow row) {
        return new EventView(row.occurredAt(), row.level(), row.category(), row.message());
    }

    private void applyBaggageJam(int severity, int durationMinutes) {
        int affectedBags = Math.max(10, severity * 35);
        jdbcTemplate.update("""
                update simulation_baggage
                set status = 'DELAYED',
                    exception_reason = ?,
                    last_updated = ?
                where id in (
                    select id
                    from simulation_baggage
                    where status <> 'LOST'
                    order by last_updated desc, id
                    limit ?
                )
                """,
                "Baggage belt jam for " + durationMinutes + " minutes",
                LocalDateTime.now(clock),
                affectedBags);
        jdbcTemplate.update("""
                update simulation_ground_operations
                set status = 'DELAYED',
                    delay_minutes = greatest(delay_minutes, ?),
                    last_updated = ?
                where operation_type = 'BAGGAGE_LOADING'
                  and status in ('PENDING', 'ACTIVE')
                """, severity * 10, LocalDateTime.now(clock));
    }

    private void applyStaffingDisruption(int severity, int durationMinutes) {
        jdbcTemplate.update("""
                update simulation_ground_operations
                set status = 'DELAYED',
                    delay_minutes = greatest(delay_minutes, ?),
                    last_updated = ?
                where operation_type in ('CHECK_IN', 'SECURITY', 'BOARDING')
                  and status in ('PENDING', 'ACTIVE')
                """, severity * 8, LocalDateTime.now(clock));
        upsertQueue("CHECK_IN", (int) countPassengersWhere("checked_in = false"), Math.max(1, 18 - severity * 3), "STAFFING_LIMITED");
        upsertQueue("SECURITY", (int) countPassengersWhere("checked_in = true and security_cleared = false"), Math.max(1, 16 - severity * 3), "STAFFING_LIMITED");
        insertEvent("WARN", "STAFFING", "Staffing disruption active for " + durationMinutes + " minutes");
    }

    private void applyGateClose(String gateCode, int severity, int durationMinutes) {
        String selectedGate = blankToDefault(gateCode, firstOpenGate());
        if (selectedGate == null) {
            return;
        }
        jdbcTemplate.update("""
                update simulation_gates
                set open = false,
                    state = 'CLOSED',
                    last_updated = ?
                where gate_code = ?
                """, LocalDateTime.now(clock), selectedGate);
        jdbcTemplate.update("""
                update simulation_flights
                set status = 'DELAYED',
                    delay_minutes = delay_minutes + ?,
                    delay_reason = ?,
                    last_updated = ?
                where gate = ?
                  and status in ('SCHEDULED', 'CHECK_IN_OPEN', 'BOARDING')
                """,
                severity * 10,
                "Gate " + selectedGate + " closed for " + durationMinutes + " minutes",
                LocalDateTime.now(clock),
                selectedGate);
    }

    private void applyGateOpen(String gateCode) {
        String selectedGate = blankToDefault(gateCode, null);
        if (selectedGate == null) {
            throw new IllegalArgumentException("Gate code is required to open a gate");
        }
        jdbcTemplate.update("""
                update simulation_gates
                set open = true,
                    state = 'AVAILABLE',
                    last_updated = ?
                where gate_code = ?
                """, LocalDateTime.now(clock), selectedGate);
    }

    private void applyRunwayHold(int severity, int durationMinutes) {
        jdbcTemplate.update("""
                update simulation_flights
                set status = 'DELAYED',
                    delay_minutes = delay_minutes + ?,
                    delay_reason = ?,
                    last_updated = ?
                where status in ('SCHEDULED', 'CHECK_IN_OPEN', 'BOARDING')
                  and departure_time <= ?
                """,
                severity * 12,
                "Runway hold for " + durationMinutes + " minutes",
                LocalDateTime.now(clock),
                requireState().simulatedTime().plusMinutes(durationMinutes));
        jdbcTemplate.update("""
                update simulation_ground_operations
                set status = 'DELAYED',
                    delay_minutes = greatest(delay_minutes, ?),
                    last_updated = ?
                where operation_type in ('BOARDING', 'BAGGAGE_LOADING', 'FUELING')
                  and status in ('PENDING', 'ACTIVE')
                """, severity * 10, LocalDateTime.now(clock));
    }

    private void refreshResourceQueues(LocalDateTime currentTime) {
        upsertQueue("CHECK_IN", (int) countPassengersWhere("checked_in = false and status <> 'MISSED_CONNECTION'"), queueCapacity("CHECK_IN"), queueStatus("CHECK_IN"));
        upsertQueue("SECURITY", (int) countPassengersWhere("checked_in = true and security_cleared = false and status <> 'MISSED_CONNECTION'"), queueCapacity("SECURITY"), queueStatus("SECURITY"));
        upsertQueue("BAGGAGE", (int) countBaggageWhere("status in ('REGISTERED', 'SCREENED', 'DELAYED')"), queueCapacity("BAGGAGE"), queueStatus("BAGGAGE"));
        upsertQueue("RUNWAY", (int) countFlightsWhere("status in ('BOARDING', 'DELAYED')"), queueCapacity("RUNWAY"), queueStatus("RUNWAY"));
        jdbcTemplate.update("""
                update simulation_resource_queues
                set updated_at = ?
                """, currentTime);
    }

    private void upsertQueue(String resourceName, int queueDepth, int capacity, String status) {
        jdbcTemplate.update("""
                insert into simulation_resource_queues (
                    resource_name,
                    queue_depth,
                    capacity,
                    status,
                    updated_at
                ) values (?, ?, ?, ?, ?)
                on conflict (resource_name) do update set
                    queue_depth = excluded.queue_depth,
                    capacity = excluded.capacity,
                    status = excluded.status,
                    updated_at = excluded.updated_at
                """,
                resourceName,
                Math.max(0, queueDepth),
                Math.max(1, capacity),
                status,
                LocalDateTime.now(clock));
    }

    private int queueCapacity(String resourceName) {
        int target = targetFlightCount();
        return switch (resourceName) {
            case "CHECK_IN" -> Math.max(18, target / 4);
            case "SECURITY" -> Math.max(16, target / 5);
            case "BAGGAGE" -> Math.max(35, target / 2);
            case "RUNWAY" -> Math.max(4, runwayCountForState());
            default -> 10;
        };
    }

    private String queueStatus(String resourceName) {
        return switch (resourceName) {
            case "CHECK_IN" -> countPassengersWhere("checked_in = false") > queueCapacity(resourceName) ? "BUSY" : "NORMAL";
            case "SECURITY" -> countPassengersWhere("checked_in = true and security_cleared = false") > queueCapacity(resourceName) ? "BUSY" : "NORMAL";
            case "BAGGAGE" -> countBaggageWhere("status in ('REGISTERED', 'SCREENED', 'DELAYED')") > queueCapacity(resourceName) ? "BUSY" : "NORMAL";
            case "RUNWAY" -> countFlightsWhere("status in ('BOARDING', 'DELAYED')") > queueCapacity(resourceName) ? "BUSY" : "NORMAL";
            default -> "NORMAL";
        };
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
                    simulation_baggage,
                    simulation_passengers,
                    simulation_ground_operations,
                    simulation_gates,
                    simulation_resource_queues,
                    simulation_events,
                    simulation_flights,
                    simulation_state,
                    bootstrap_status,
                    import_weather_snapshots,
                    import_openflights_routes,
                    import_openflights_planes,
                    import_openflights_airlines,
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
        return gateFor(index, 6);
    }

    private String gateFor(int index, int gateCount) {
        int safeGateCount = Math.max(1, gateCount);
        int terminalIndex = (index / safeGateCount) % 3;
        char terminal = (char) ('A' + terminalIndex);
        return terminal + String.valueOf((index % safeGateCount) + 1);
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

    private List<List<String>> readDatRows(Path file) {
        CsvParser parser = new CsvParser();
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .filter(line -> !line.isBlank())
                    .map(parser::parseLine)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read OpenFlights data: " + file, exception);
        }
    }

    private void batchUpdate(String sql, List<Object[]> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int chunkSize = 1000;
        for (int start = 0; start < rows.size(); start += chunkSize) {
            int end = Math.min(start + chunkSize, rows.size());
            jdbcTemplate.batchUpdate(sql, rows.subList(start, end));
        }
    }

    private FlightRow requireFlight(long flightId) {
        List<FlightRow> flights = jdbcTemplate.query("""
                select id, source_row_id, flight_number, airline, origin_label, destination_label,
                       departure_time, arrival_time, status, delay_minutes, gate, runway, weather_notes,
                       route_source, aircraft_code, aircraft_name, passenger_count, baggage_count, direction, delay_reason
                from simulation_flights
                where id = ?
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
                        rs.getString("weather_notes"),
                        rs.getString("route_source"),
                        rs.getString("aircraft_code"),
                        rs.getString("aircraft_name"),
                        rs.getInt("passenger_count"),
                        rs.getInt("baggage_count"),
                        rs.getString("direction"),
                        rs.getString("delay_reason")
                ),
                flightId);
        if (flights.isEmpty()) {
            throw new IllegalArgumentException("Unknown flight id: " + flightId);
        }
        return flights.getFirst();
    }

    private String airportLabel(String code) {
        if (code == null || code.isBlank()) {
            return "Unknown airport";
        }
        try {
            AirportRow airport = airportByCode(code);
            return airport.code() + " - " + airport.city();
        } catch (IllegalArgumentException ignored) {
            return code;
        }
    }

    private String flightNumberFor(RouteTemplateRow template, int index) {
        String airlineCode = blankToDefault(template.airlineCode(), "SIM")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        if (airlineCode.length() > 3) {
            airlineCode = airlineCode.substring(0, 3);
        }
        return airlineCode + String.format(Locale.ROOT, "%04d", 100 + (index % 9000));
    }

    private String directionFor(RouteTemplateRow template, String activeAirportCode) {
        if (activeAirportCode.equalsIgnoreCase(template.sourceAirport())) {
            return "DEPARTURE";
        }
        if (activeAirportCode.equalsIgnoreCase(template.destinationAirport())) {
            return "ARRIVAL";
        }
        return "TURNAROUND";
    }

    private double durationHoursFor(String sourceAirport, String destinationAirport, String aircraftCode, int index) {
        AirportRow source = airportByCodeOrNull(sourceAirport);
        AirportRow destination = airportByCodeOrNull(destinationAirport);
        if (source != null && destination != null
                && source.latitude() != null && source.longitude() != null
                && destination.latitude() != null && destination.longitude() != null) {
            double distanceKm = haversineKm(source.latitude(), source.longitude(), destination.latitude(), destination.longitude());
            return clampDouble((distanceKm / cruiseSpeedKmh(aircraftCode)) + 0.45, 0.6, 14.0);
        }
        return 1.0 + ((index % 9) * 0.22);
    }

    private AirportRow airportByCodeOrNull(String airportCode) {
        try {
            return airportByCode(airportCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private double cruiseSpeedKmh(String aircraftCode) {
        String code = blankToDefault(aircraftCode, "").toUpperCase(Locale.ROOT);
        if (code.startsWith("AT") || code.startsWith("DH") || code.startsWith("SF")) {
            return 500.0;
        }
        if (code.startsWith("CR") || code.startsWith("E")) {
            return 720.0;
        }
        return 820.0;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private int passengerCountFor(String aircraftCode, int index) {
        int capacity = aircraftCapacity(aircraftCode);
        double variation = 0.88 + ((index % 7) * 0.035);
        return Math.max(18, (int) Math.round(capacity * safeLoadFactor() * variation));
    }

    private int aircraftCapacity(String aircraftCode) {
        String code = blankToDefault(aircraftCode, "").toUpperCase(Locale.ROOT);
        if (code.contains("380")) {
            return 510;
        }
        if (code.contains("747")) {
            return 410;
        }
        if (code.contains("777") || code.contains("350") || code.contains("340")) {
            return 320;
        }
        if (code.contains("787") || code.contains("330") || code.contains("767")) {
            return 260;
        }
        if (code.contains("757") || code.contains("321")) {
            return 210;
        }
        if (code.contains("320") || code.contains("737") || code.contains("738") || code.contains("739")) {
            return 178;
        }
        if (code.contains("319") || code.contains("318") || code.contains("E9")) {
            return 130;
        }
        if (code.contains("AT") || code.contains("DH") || code.contains("CR")) {
            return 76;
        }
        return 150;
    }

    private int baggageCountFor(int passengerCount, int index) {
        double variation = 0.92 + ((index % 5) * 0.04);
        return Math.max(0, (int) Math.round(passengerCount * safeBagRate() * variation));
    }

    private int initialDelayMinutes(WeatherSeverity severity, int index) {
        if (severity.rank() >= WeatherSeverity.GROUND_STOP.rank() && index % 3 == 0) {
            return 45;
        }
        if (severity.rank() >= WeatherSeverity.SEVERE.rank() && index % 4 == 0) {
            return 25;
        }
        if (severity.rank() >= WeatherSeverity.CAUTION.rank() && index % 7 == 0) {
            return 10;
        }
        return 0;
    }

    private int targetFlightCount() {
        int configuredTarget = properties.getTargetDailyFlights();
        if (configuredTarget > 0) {
            return clamp(configuredTarget, 1, 1000);
        }
        return clamp(properties.getDemoFlightCount(), 1, 1000);
    }

    private String safeTrafficProfile() {
        return blankToDefault(properties.getTrafficProfile(), "BUSY").toUpperCase(Locale.ROOT);
    }

    private double safeLoadFactor() {
        return clampDouble(properties.getPassengerLoadFactor(), 0.35, 1.0);
    }

    private double safeBagRate() {
        return clampDouble(properties.getBagRate(), 0.0, 1.4);
    }

    private int gateCountFor(int targetFlights) {
        if (targetFlights >= 700) {
            return 56;
        }
        if (targetFlights >= 180) {
            return 28;
        }
        if (targetFlights >= 80) {
            return 16;
        }
        return 8;
    }

    private String terminalFor(int index) {
        return "T" + ((index / 12) + 1);
    }

    private PassengerState passengerStateFor(String flightStatus) {
        return switch (flightStatus) {
            case "CHECK_IN_OPEN" -> new PassengerState("CHECKED_IN", true, false, false, false);
            case "BOARDING" -> new PassengerState("BOARDING", true, true, false, false);
            case "DELAYED" -> new PassengerState("WAITING", true, true, false, false);
            case "DEPARTED", "ARRIVED" -> new PassengerState("BOARDED", true, true, true, false);
            case "CANCELLED" -> new PassengerState("MISSED_CONNECTION", true, false, false, true);
            default -> new PassengerState("BOOKED", false, false, false, false);
        };
    }

    private BaggageStateRow baggageStateFor(String flightStatus) {
        return switch (flightStatus) {
            case "CHECK_IN_OPEN" -> new BaggageStateRow("SCREENED", null);
            case "BOARDING" -> new BaggageStateRow("LOADED", null);
            case "DELAYED" -> new BaggageStateRow("SCREENED", "Flight delayed");
            case "DEPARTED" -> new BaggageStateRow("IN_TRANSIT", null);
            case "ARRIVED" -> new BaggageStateRow("DELIVERED", null);
            case "CANCELLED" -> new BaggageStateRow("DELAYED", "Flight cancelled");
            default -> new BaggageStateRow("REGISTERED", null);
        };
    }

    private String passengerCode(long flightId, int index) {
        return "PAX-" + flightId + "-" + (index + 1);
    }

    private String baggageTag(long flightId, int index) {
        return "BAG-" + flightId + "-" + String.format(Locale.ROOT, "%04d", index + 1);
    }

    private String baggageBeltFor(long flightId) {
        return "B" + ((flightId % 6) + 1);
    }

    private String codeLike(String value, String fallback) {
        String normalized = blankToDefault(value, fallback).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.length() >= 3) {
            return normalized.substring(0, 3);
        }
        return fallback;
    }

    private String airlineCodeFromFlight(String flightNumber) {
        String normalized = blankToDefault(flightNumber, "SIM").replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if (normalized.length() >= 2) {
            return normalized.substring(0, Math.min(3, normalized.length()));
        }
        return "SIM";
    }

    private String firstOpenGate() {
        List<String> gates = jdbcTemplate.query("""
                select gate_code
                from simulation_gates
                where open = true
                order by gate_code
                limit 1
                """, (rs, rowNum) -> rs.getString("gate_code"));
        return gates.isEmpty() ? null : gates.getFirst();
    }

    private int runwayCountForState() {
        SimulationStateRow state = loadState();
        if (state == null) {
            return 2;
        }
        AirportRow airport = airportByCodeOrNull(state.activeAirportCode());
        if (airport == null) {
            return 2;
        }
        Integer value = jdbcTemplate.queryForObject("""
                select count(*)
                from import_runways
                where airport_ref = ? or airport_ident = ?
                """, Integer.class, airport.id(), airport.ident());
        return Math.max(1, value == null ? 2 : value);
    }

    private long countFlightsWhere(String condition) {
        return countWhere("simulation_flights", condition);
    }

    private long countPassengersWhere(String condition) {
        return countWhere("simulation_passengers", condition);
    }

    private long countBaggageWhere(String condition) {
        return countWhere("simulation_baggage", condition);
    }

    private long countGatesWhere(String condition) {
        return countWhere("simulation_gates", condition);
    }

    private long countGroundWhere(String condition) {
        return countWhere("simulation_ground_operations", condition);
    }

    private long countWhere(String tableName, String condition) {
        Long value = jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + condition, Long.class);
        return value == null ? 0 : value;
    }

    private String openFlightsValue(String value) {
        if (value == null || value.isBlank() || "\\N".equals(value)) {
            return null;
        }
        return value.trim();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Long nullableLong(String value) {
        String normalized = openFlightsValue(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer nullableInteger(String value, int defaultValue) {
        String normalized = openFlightsValue(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
            String weatherNotes,
            String routeSource,
            String aircraftCode,
            String aircraftName,
            int passengerCount,
            int baggageCount,
            String direction,
            String delayReason
    ) {
    }

    private record RouteTemplateRow(
            long rowId,
            String airlineCode,
            String airlineName,
            String sourceAirport,
            String destinationAirport,
            String aircraftCode,
            String aircraftName,
            String routeSource
    ) {
    }

    private record PassengerState(
            String status,
            boolean checkedIn,
            boolean securityCleared,
            boolean boarded,
            boolean missedConnection
    ) {
    }

    private record BaggageStateRow(String status, String exceptionReason) {
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
