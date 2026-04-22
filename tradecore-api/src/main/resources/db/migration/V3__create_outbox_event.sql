create table outbox_event
(
    event_id        varchar(64) primary key,
    aggregate_type  varchar(50)  not null,
    aggregate_id    varchar(64)  not null,
    event_type      varchar(100) not null,
    payload         jsonb        not null,
    status          varchar(20)  not null,
    attempt_count   integer      not null default 0,
    last_error      text,
    created_at      timestamptz  not null,
    published_at    timestamptz
);

create index idx_outbox_event_status_created_at
    on outbox_event (status, created_at);

create index idx_outbox_event_aggregate
    on outbox_event (aggregate_type, aggregate_id);