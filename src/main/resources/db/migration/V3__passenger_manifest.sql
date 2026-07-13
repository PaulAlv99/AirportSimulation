alter table simulation_passengers
    add column if not exists full_name text not null default 'Passenger',
    add column if not exists seat_number text,
    add column if not exists travel_document text;

update simulation_passengers
set full_name = (array[
        'Ana Silva',
        'Miguel Santos',
        'Sofia Costa',
        'Joao Ferreira',
        'Mariana Oliveira',
        'Pedro Almeida',
        'Carolina Martins',
        'Tiago Rodrigues',
        'Beatriz Pereira',
        'Rafael Gomes',
        'Ines Carvalho',
        'Luis Ribeiro',
        'Marta Fernandes',
        'Nuno Lopes',
        'Clara Moreira',
        'Andre Cardoso'
    ])[(((id - 1) % 16) + 1)::int],
    seat_number = ((id - 1) % 32 + 1)::text || chr(65 + (((id - 1) / 32) % 6)::int),
    travel_document = 'P' || lpad(id::text, 7, '0')
where full_name = 'Passenger';

create index if not exists idx_simulation_passengers_flight_status on simulation_passengers (flight_id, status);
