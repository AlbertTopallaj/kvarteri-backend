# Kvarteri-backend

## What is Kvarteri-backend?

Kvarteri backend is the backend for the system Kvarteri. Kvarteri is a system for apartments complexs in Sweden which gathers information messages, booking of communityrooms, parking, garage, laundry in one place. The boardmembers can send out important messages to the whole complex. Meanwhile the residents of the complex can book the apartments complexs different options of public space.

## What features does the Kvarteri-backend have?

- Booking of parking lot
- Booking of garage
- Booking of community room
- Board member can send out messages
- Resident can write/open a ticket to Board members
- Board member can contact Resident individually
- The Admin (dev) gives access to the system.

## Tech-stack
- Java/Spring Boot
- PostgreSQL

## Status

Under development. Stay tuned



## Running

Environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DDL_AUTO` (default `validate`; the schema is created by Flyway migrations in `db/migration`),
`KVARTERI_ADMIN_EMAIL` + `KVARTERI_ADMIN_PASSWORD` (creates the first admin on startup, min 10 chars).

## API (HTTP Basic auth)

| Endpoint | Access |
|---|---|
| `POST /api/admin/residents` | ADMIN |
| `GET /api/messages`, `POST /api/messages` | all / BOARD_MEMBER, ADMIN |
| `GET/POST /api/tickets`, `POST /api/tickets/{id}/close` | all (own tickets) / close: BOARD_MEMBER, ADMIN |
| `GET /api/communityrooms`, `/api/laundries`; `GET/POST /{id}/bookings`; `DELETE /api/bookings/{id}` | authenticated (cancel: owner or staff) |
| `POST /api/admin/communityrooms|laundries|parkings|garages` | ADMIN |
| `GET/POST /api/tickets/{id}/replies` | ticket creator, BOARD_MEMBER, ADMIN |
| `/api/parkings`, `/api/garages` (`/{id}/claim`, `/{id}/release`) | authenticated |

Login is throttled: 5 failed attempts per client ip + username gives 429 for 15 minutes (in-memory, per instance).
Bookings are time slots (max 12 h, up to 60 days ahead); overlapping bookings give 409.
