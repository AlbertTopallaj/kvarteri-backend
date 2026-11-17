alter table tickets add column kind varchar(255) not null default 'ERRAND';
update tickets set status = 'PENDING' where status = 'OPEN';
update tickets set status = 'DONE' where status = 'CLOSED';

create table notifications (
    id uuid primary key,
    recipient_id uuid not null references residents (id),
    text varchar(1000) not null,
    created_at timestamp(6) not null,
    seen boolean not null
);
create index idx_notifications_recipient on notifications (recipient_id, created_at);
