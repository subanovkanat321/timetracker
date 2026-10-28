create table employee (
    id         bigint generated always as identity primary key,
    name       varchar(100) not null,
    created_at timestamptz  not null default now()
);
-- Имя уникально без учёта регистра: в отчёте не будет двух «Иванов», которых не различить
create unique index employee_name_uq on employee (lower(name));

create table time_entry (
    id          bigint generated always as identity primary key,
    employee_id bigint       not null references employee (id),
    description varchar(500) not null,
    started_at  timestamptz  not null,
    ended_at    timestamptz,
    constraint time_entry_period_ck check (ended_at is null or ended_at >= started_at)
);

-- Не больше одного идущего таймера на сотрудника. Правило держит база, а не код:
-- два одновременных «Старта» не создадут два таймера. Второй INSERT с ON CONFLICT
-- DO NOTHING (TimerService.start) дождётся первого и ничего не вставит
create unique index time_entry_one_running_uq on time_entry (employee_id) where ended_at is null;

create index time_entry_started_idx on time_entry (started_at);
