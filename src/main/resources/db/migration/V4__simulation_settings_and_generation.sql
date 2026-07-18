create table if not exists simulation_settings (
    id smallint primary key,
    operating_start_time time without time zone not null,
    operating_end_time time without time zone not null,
    traffic_profile text not null,
    target_daily_flights integer not null,
    passenger_load_factor double precision not null,
    bag_rate double precision not null,
    staffing_multiplier double precision not null,
    delay_probability double precision not null,
    baggage_exception_probability double precision not null,
    passenger_no_show_probability double precision not null,
    ground_jitter_minutes integer not null,
    planning_horizon_minutes integer not null,
    retention_days integer not null,
    random_seed bigint not null,
    updated_at timestamp without time zone not null
);

alter table simulation_state
    add column if not exists run_seed bigint not null default 0,
    add column if not exists generation_cursor timestamp without time zone,
    add column if not exists generation_horizon_end timestamp without time zone,
    add column if not exists next_opening_at timestamp without time zone,
    add column if not exists last_tick_at timestamp without time zone,
    add column if not exists last_processed_minute timestamp without time zone,
    add column if not exists generated_flights bigint not null default 0;
