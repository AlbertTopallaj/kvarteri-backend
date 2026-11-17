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

Environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DDL_AUTO` (use `validate` in production),
`KVARTERI_ADMIN_EMAIL` + `KVARTERI_ADMIN_PASSWORD` (creates the first admin on startup, min 10 chars).

## API (HTTP Basic auth)

| Endpoint | Access |
|---|---|
| `POST /api/admin/residents` | ADMIN |
| `GET /api/messages`, `POST /api/messages` | all / BOARD_MEMBER, ADMIN |
| `GET/POST /api/tickets`, `POST /api/tickets/{id}/close` | all (own tickets) / close: BOARD_MEMBER, ADMIN |
| `/api/communityrooms`, `/api/laundries` (`/{id}/book`, `/{id}/cancel`) | authenticated |
| `/api/parkings`, `/api/garages` (`/{id}/claim`, `/{id}/release`) | authenticated |
