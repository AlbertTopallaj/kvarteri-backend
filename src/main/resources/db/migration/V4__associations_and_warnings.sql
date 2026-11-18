create table associations (
    id uuid primary key,
    name varchar(255) not null unique
);

-- Everything that existed before associations belongs to one default association.
insert into associations (id, name) values ('00000000-0000-0000-0000-000000000001', 'Standardförening');

alter table residents add column association_id uuid references associations (id);
update residents set association_id = '00000000-0000-0000-0000-000000000001';
alter table residents alter column association_id set not null;
create index idx_residents_association on residents (association_id);

alter table communityrooms add column association_id uuid references associations (id);
update communityrooms set association_id = '00000000-0000-0000-0000-000000000001';
alter table communityrooms alter column association_id set not null;

alter table laundries add column association_id uuid references associations (id);
update laundries set association_id = '00000000-0000-0000-0000-000000000001';
alter table laundries alter column association_id set not null;

alter table parking_spots add column association_id uuid references associations (id);
update parking_spots set association_id = '00000000-0000-0000-0000-000000000001';
alter table parking_spots alter column association_id set not null;
alter table parking_spots drop constraint uq_parking_spots_kind_name;
alter table parking_spots add constraint uq_parking_spots_assoc_kind_name unique (association_id, kind, name);

create table warnings (
    id uuid primary key,
    resident_id uuid not null references residents (id),
    issued_by_id uuid not null references residents (id),
    reason varchar(1000) not null,
    created_at timestamp(6) not null
);
create index idx_warnings_resident on warnings (resident_id, created_at);
