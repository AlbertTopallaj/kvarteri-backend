create table residents (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    name varchar(255) not null,
    apartment_number varchar(255),
    role varchar(255) not null,
    enabled boolean not null
);

create table communityrooms (
    id uuid primary key,
    version bigint not null,
    name varchar(255) not null,
    price integer not null,
    capacity integer not null
);

create table laundries (
    id uuid primary key,
    version bigint not null,
    name varchar(255) not null
);

create table parkings (
    id uuid primary key,
    version bigint not null,
    parking_number integer not null unique,
    price integer not null,
    tenant_id uuid references residents (id)
);

create table garages (
    id uuid primary key,
    version bigint not null,
    garage_number integer not null unique,
    price integer not null,
    owner_id uuid references residents (id)
);

create table bookings (
    id uuid primary key,
    resource_type varchar(255) not null,
    resource_id uuid not null,
    resident_id uuid not null references residents (id),
    start_time timestamp(6) not null,
    end_time timestamp(6) not null
);
create index idx_bookings_resource on bookings (resource_type, resource_id, start_time);

create table messages (
    id uuid primary key,
    title varchar(255) not null,
    body varchar(4000) not null,
    author_id uuid not null references residents (id),
    recipient_id uuid references residents (id),
    created_at timestamp(6) not null
);

create table tickets (
    id uuid primary key,
    subject varchar(255) not null,
    body varchar(4000) not null,
    created_by_id uuid not null references residents (id),
    status varchar(255) not null,
    created_at timestamp(6) not null
);

create table ticket_replies (
    id uuid primary key,
    ticket_id uuid not null references tickets (id),
    author_id uuid not null references residents (id),
    body varchar(4000) not null,
    created_at timestamp(6) not null
);
