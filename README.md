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
| `POST /api/admin/communityrooms|laundries` | ADMIN |
| `GET/POST /api/tickets/{id}/replies` | ticket creator, BOARD_MEMBER, ADMIN |
| `GET /api/parking-spots`; `POST/DELETE /api/parking-spots/{id}/queue` | authenticated |
| `POST /api/parking-spots`, `DELETE`/`PATCH /api/parking-spots/{id}`, `POST /{id}/assign` or `/terminate`, `GET /api/residents` | BOARD_MEMBER, ADMIN |
| `GET/POST /api/admin/associations`; `GET /api/admin/residents?associationId=` | ADMIN |
| `DELETE /api/residents/{id}`, `POST/GET /api/residents/{id}/warnings` | BOARD_MEMBER (own association, RESIDENT accounts only), ADMIN (anyone) |
| `GET /api/me/warnings` | authenticated |

Every user belongs to one association. Rooms, laundries, parking spots, tickets, messages, residents and bookings are
only visible within the user's own association; other associations' data answers 404. ADMIN creates associations and
accounts in any of them (`associationId`, default: the admin's own). Existing data is migrated into a default
association (`Standardförening`).

Login is throttled: 5 failed attempts per client ip + username gives 429 for 15 minutes (in-memory, per instance).
Bookings are time slots (max 12 h, up to 60 days ahead); overlapping bookings give 409.

## Docker

```
cp .env.example .env   # set passwords
docker compose up --build
```

The API is served on `127.0.0.1:8080`; PostgreSQL is only reachable inside the compose network. Because the API
uses HTTP Basic, run it behind a reverse proxy with TLS in any real deployment.
