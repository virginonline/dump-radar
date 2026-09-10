create table t_candidates
(
    id           text primary key,
    base_asset   text    not null,
    exchanges    text    not null,
    source       text    not null,
    state        text    not null,
    anchor_high  text    not null,
    pump_start   text    not null,
    detected_at  integer not null,
    deadline     integer not null,
    confirmed_at integer,
    updated_at   integer not null
);
create index idx_candidates_state on t_candidates (state);

create table t_events
(
    id           integer primary key autoincrement,
    candidate_id text    not null references t_candidates (id),
    event_type   text    not null,
    payload      text    not null,
    occurred_at  integer not null
);
create index idx_events_candidate on t_events (candidate_id, occurred_at);


create table t_candle_1m
(
    symbol    text    not null,
    open_time integer not null,
    open      text    not null,
    high      text    not null,
    low       text    not null,
    close     text    not null,
    volume    text    not null,
    primary key (symbol, open_time)
) without rowid;

create table t_symbols_seen
(
    symbol     text primary key,
    first_seen integer not null
);
