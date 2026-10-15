alter table tool_job
    add column input text;
alter table tool_job
    add column started_at timestamp with time zone;
alter table tool_job
    add column expires_at timestamp with time zone;
alter table tool_job
    add column lease_until timestamp with time zone;
alter table tool_job
    add column lease_token varchar(64);
alter table tool_job
    add column error_code varchar(64);
create index tool_job_queue on tool_job (status, created_at);
create index tool_job_expiry on tool_job (expires_at);

-- Serialize admission and claims across application instances without holding a lock during execution.
create table tool_job_gate
(
    id integer primary key
);
insert into tool_job_gate(id)
values (1);
update tool_job
set status      = 'FAILED',
    error       = 'Legacy job has no saved input',
    error_code  = 'INTERRUPTED',
    finished_at = current_timestamp
where status in ('QUEUED', 'RUNNING');
update tool_job
set expires_at = current_timestamp
where expires_at is null;
