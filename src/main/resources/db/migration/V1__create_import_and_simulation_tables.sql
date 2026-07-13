create table if not exists import_countries (
    row_id bigint primary key,
    code text not null,
    name text not null,
    continent text not null
);

create table if not exists import_regions (
    row_id bigint primary key,
    code text not null,
    local_code text,
    name text not null,
    continent text,
    iso_country text not null
);

create table if not exists import_airports (
    id bigint primary key,
    ident text,
    type text,
    name text not null,
    latitude_deg double precision,
    longitude_deg double precision,
    elevation_ft integer,
    continent text,
    iso_country text,
    iso_region text,
    municipality text,
    scheduled_service text,
    gps_code text,
    iata_code text,
    local_code text,
    extra_1 text,
    extra_2 text,
    extra_3 text
);

create table if not exists import_runways (
    id bigint primary key,
    airport_ref bigint,
    airport_ident text,
    length_ft integer,
    width_ft integer,
    surface text,
    lighted integer,
    closed integer,
    le_ident text
);

create table if not exists import_navaids (
    id bigint primary key,
    filename text,
    ident text,
    name text,
    type text,
    frequency_khz integer,
    latitude_deg double precision,
    longitude_deg double precision,
    elevation_ft integer,
    iso_country text,
    magnetic_variation_deg double precision,
    usage_type text,
    power text,
    associated_airport text
);

create table if not exists import_weather_snapshots (
    airport_id text not null,
    observed_at timestamp without time zone not null,
    temperature_celsius double precision not null,
    feels_like_celsius double precision not null,
    wind_speed_kmh double precision not null,
    wind_gust_kmh double precision not null,
    wind_direction_degrees integer not null,
    rain_mm_per_hour double precision not null,
    snow_mm_per_hour double precision not null,
    hail boolean not null,
    thunderstorm boolean not null,
    visibility_meters integer not null,
    fog boolean not null,
    cloud_coverage_percent integer not null,
    ceiling_meters integer not null,
    cloud_label text not null,
    runway_surface text not null,
    weather_severity text not null
);

create table if not exists import_airline_flights (
    row_id bigint primary key,
    airline text not null,
    flight text not null,
    source_city text not null,
    departure_time text not null,
    stops text not null,
    arrival_time text not null,
    destination_city text not null,
    class text not null,
    duration double precision not null,
    days_left integer not null,
    price integer not null
);

create table if not exists simulation_state (
    id smallint primary key,
    lifecycle_state text not null,
    simulated_time timestamp without time zone not null,
    multiplier text not null,
    running boolean not null,
    active_airport_code text,
    updated_at timestamp without time zone not null
);

create table if not exists simulation_flights (
    id bigserial primary key,
    source_row_id bigint,
    flight_number text not null,
    airline text not null,
    origin_label text not null,
    destination_label text not null,
    departure_time timestamp without time zone not null,
    arrival_time timestamp without time zone not null,
    status text not null,
    delay_minutes integer not null default 0,
    gate text,
    runway text,
    weather_notes text,
    last_updated timestamp without time zone not null
);

create table if not exists simulation_events (
    id bigserial primary key,
    occurred_at timestamp without time zone not null,
    level text not null,
    category text not null,
    message text not null
);

create table if not exists bootstrap_status (
    bootstrap_key text primary key,
    bootstrap_value text not null,
    updated_at timestamp without time zone not null
);

create index if not exists idx_import_airports_iata_code on import_airports (iata_code);
create index if not exists idx_import_airports_ident on import_airports (ident);
create index if not exists idx_import_weather_airport_observed on import_weather_snapshots (airport_id, observed_at desc);
create index if not exists idx_import_airline_flights_row_id on import_airline_flights (row_id);
create index if not exists idx_import_runways_airport_ref on import_runways (airport_ref);
create index if not exists idx_import_runways_airport_ident on import_runways (airport_ident);
create index if not exists idx_simulation_flights_status on simulation_flights (status);
create index if not exists idx_simulation_events_occurred on simulation_events (occurred_at desc);
