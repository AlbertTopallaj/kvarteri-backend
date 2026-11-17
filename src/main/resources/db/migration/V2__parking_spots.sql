create table parking_spots (
    id uuid primary key,
    version bigint not null,
    kind varchar(255) not null,
    name varchar(255) not null,
    price integer not null,
    tenant_id uuid references residents (id),
    constraint uq_parking_spots_kind_name unique (kind, name)
);

create table spot_queue_entries (
    id uuid primary key,
    spot_id uuid not null references parking_spots (id),
    resident_id uuid not null references residents (id),
    created_at timestamp(6) not null,
    constraint uq_spot_queue_spot_resident unique (spot_id, resident_id)
);
create index idx_spot_queue_spot on spot_queue_entries (spot_id, created_at);

-- Keep existing data from the old claim/release model.
insert into parking_spots (id, version, kind, name, price, tenant_id)
    select id, version, 'PARKING', 'P' || parking_number, price, tenant_id from parkings;
insert into parking_spots (id, version, kind, name, price, tenant_id)
    select id, version, 'GARAGE', 'G' || garage_number, price, owner_id from garages;

drop table parkings;
drop table garages;
