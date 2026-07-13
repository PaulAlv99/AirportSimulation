create table if not exists import_openflights_airlines (
    airline_id bigint primary key,
    name text not null,
    alias text,
    iata_code text,
    icao_code text,
    callsign text,
    country text,
    active text
);

create table if not exists import_openflights_routes (
    id bigserial primary key,
    airline_code text,
    airline_id text,
    source_airport text not null,
    source_airport_id text,
    destination_airport text not null,
    destination_airport_id text,
    codeshare text,
    stops integer not null default 0,
    equipment text
);

create table if not exists import_openflights_planes (
    id bigserial primary key,
    name text not null,
    iata_code text,
    icao_code text
);

alter table simulation_flights
    add column if not exists route_source text,
    add column if not exists aircraft_code text,
    add column if not exists aircraft_name text,
    add column if not exists passenger_count integer not null default 0,
    add column if not exists baggage_count integer not null default 0,
    add column if not exists direction text,
    add column if not exists delay_reason text;

create table if not exists simulation_gates (
    id bigserial primary key,
    gate_code text not null unique,
    terminal text not null,
    state text not null,
    open boolean not null default true,
    flight_id bigint references simulation_flights(id) on delete set null,
    passenger_queue integer not null default 0,
    baggage_queue integer not null default 0,
    last_updated timestamp without time zone not null
);

create table if not exists simulation_passengers (
    id bigserial primary key,
    flight_id bigint not null references simulation_flights(id) on delete cascade,
    passenger_code text not null unique,
    status text not null,
    checked_in boolean not null default false,
    security_cleared boolean not null default false,
    boarded boolean not null default false,
    missed_connection boolean not null default false,
    baggage_count integer not null default 0,
    last_updated timestamp without time zone not null
);

create table if not exists simulation_baggage (
    id bigserial primary key,
    flight_id bigint not null references simulation_flights(id) on delete cascade,
    passenger_id bigint references simulation_passengers(id) on delete set null,
    tag text not null unique,
    status text not null,
    belt text,
    exception_reason text,
    last_updated timestamp without time zone not null
);

create table if not exists simulation_ground_operations (
    id bigserial primary key,
    flight_id bigint references simulation_flights(id) on delete cascade,
    gate_code text,
    operation_type text not null,
    status text not null,
    started_at timestamp without time zone,
    due_at timestamp without time zone,
    completed_at timestamp without time zone,
    delay_minutes integer not null default 0,
    last_updated timestamp without time zone not null
);

create table if not exists simulation_resource_queues (
    resource_name text primary key,
    queue_depth integer not null default 0,
    capacity integer not null default 0,
    status text not null,
    updated_at timestamp without time zone not null
);

create index if not exists idx_openflights_routes_source on import_openflights_routes (source_airport);
create index if not exists idx_openflights_routes_destination on import_openflights_routes (destination_airport);
create index if not exists idx_openflights_airlines_iata on import_openflights_airlines (iata_code);
create index if not exists idx_openflights_planes_iata on import_openflights_planes (iata_code);
create index if not exists idx_simulation_flights_departure on simulation_flights (departure_time);
create index if not exists idx_simulation_baggage_status on simulation_baggage (status);
create index if not exists idx_simulation_baggage_flight on simulation_baggage (flight_id);
create index if not exists idx_simulation_passengers_flight on simulation_passengers (flight_id);
create index if not exists idx_simulation_passengers_status on simulation_passengers (status);
create index if not exists idx_simulation_gates_state on simulation_gates (state);
create index if not exists idx_simulation_ground_status on simulation_ground_operations (status);
