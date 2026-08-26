# Airline Recovery Platform

A full-stack airline disruption recovery system built with **Java 21, Spring Boot, React, TypeScript, PostgreSQL, Kafka, Docker, Gradle, JUnit, and Mockito**.

The application models a real airline operations workflow: passengers can be booked on flights, operators can delay or cancel flights, and disruption events are published through Kafka so affected passengers can be automatically rebooked onto eligible replacement flights.

---

## Business Problem

Flight cancellations and delays can affect many passengers at once. A recovery system should be able to:

- manage flight schedules and flight status
- create passenger bookings
- prevent bookings on cancelled, departed, or sold-out flights
- detect flight disruptions
- asynchronously trigger recovery workflows
- find eligible replacement flights
- automatically rebook affected passengers
- protect seat inventory from concurrent booking requests
- safely handle duplicate Kafka events
- retry failed Kafka messages and route unrecoverable events to a Dead Letter Topic

This project demonstrates those workflows end to end.

---

## Architecture

```text
                      React + TypeScript
                             |
                             | REST / JSON
                             v
                    Spring Boot REST API
                             |
              +--------------+--------------+
              |                             |
              v                             v
         PostgreSQL                    Kafka Producer
   Flights / Bookings /              flight.disrupted
    Processed Events                       |
                                           v
                                  Kafka Consumer
                                           |
                                           v
                                  RebookingService
                                           |
                        +------------------+------------------+
                        |                                     |
                        v                                     v
                 Find affected                       Find replacement
                   bookings                              flights
                        |                                     |
                        +------------------+------------------+
                                           |
                                           v
                                  Reserve seat atomically
                                           |
                                           v
                                  Update booking status
                                           |
                                           v
                                      PostgreSQL
```

### Main architectural idea

- **React** handles the operations dashboard and user interaction.
- **Spring Boot REST APIs** handle synchronous operations such as flight management and booking creation.
- **PostgreSQL** stores transactional airline data.
- **Kafka** handles asynchronous disruption events.
- **RebookingService** processes disruption events and recovers affected passengers.

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Hibernate
- Bean Validation
- Spring Kafka
- PostgreSQL
- H2 for tests
- Gradle
- JUnit 5
- Mockito
- Spring Boot Actuator

### Frontend

- React
- TypeScript
- Vite
- Axios
- React Router
- CSS

### Infrastructure

- Docker
- PostgreSQL 16
- Apache Kafka
- Git / GitHub

---

## Core Features

### Flight Management

The application supports:

- creating flights
- viewing all flights
- retrieving a flight by ID
- searching by origin and destination
- updating flight status
- tracking available seat inventory

Example statuses:

```text
SCHEDULED
ON_TIME
DELAYED
CANCELLED
DEPARTED
ARRIVED
```

---

### Passenger Booking

Passengers can be booked onto eligible flights.

Before a booking is accepted, the backend verifies that the flight:

- exists
- is not cancelled
- has not departed
- has not arrived
- still has available seats

Booking statuses include:

```text
CONFIRMED
REBOOKING_REQUIRED
REBOOKED
CANCELLED
```

---

## Concurrency-Safe Seat Reservation

A normal Java read-modify-write approach can allow two concurrent requests to reserve the final available seat.

Instead, the application uses an atomic database operation:

```sql
UPDATE flights
SET available_seats = available_seats - 1
WHERE id = :flightId
  AND available_seats > 0;
```

The affected row count determines whether the reservation succeeded.

This protects the system from overselling seats during concurrent requests.

A manual concurrency test was performed with a flight containing exactly one remaining seat:

```text
Request A -> HTTP 201 Created
Request B -> HTTP 409 Booking Conflict
Final available seats -> 0
Bookings created -> 1
```

---

## Kafka Flight Disruption Events

When a flight is changed to `CANCELLED` or `DELAYED`, the backend creates a disruption event.

Example:

```json
{
  "eventId": "unique-event-id",
  "flightId": 10,
  "flightNumber": "AA902",
  "origin": "DFW",
  "destination": "SEA",
  "status": "CANCELLED",
  "occurredAt": "2026-08-25T18:30:00"
}
```

The event is published to:

```text
flight.disrupted
```

The flight ID is used as the Kafka key so events for the same flight maintain partition-level ordering.

---

## Automatic Passenger Rebooking

The Kafka consumer receives a `FlightDisruptedEvent` and invokes the recovery workflow.

The rebooking algorithm:

1. Finds confirmed bookings on the disrupted flight.
2. Searches for flights with the same origin and destination.
3. Excludes the disrupted flight.
4. Accepts only `SCHEDULED` or `ON_TIME` flights.
5. Requires available seats.
6. Requires a later departure time.
7. Sorts eligible flights by departure time.
8. Selects the earliest available option.
9. Atomically reserves a seat.
10. Moves the passenger to the replacement flight.
11. Changes the booking status to `REBOOKED`.

Example:

```text
PAX-5001
AA902 - DFW -> SEA
CONFIRMED
        |
        | AA902 cancelled
        v
flight.disrupted
        |
        v
RebookingService
        |
        v
AA903 selected
        |
        v
PAX-5001 -> AA903
Status -> REBOOKED
```

---

## Kafka Idempotency

Kafka may redeliver the same event, so the consumer must be safe to execute more than once.

Each disruption event contains a unique:

```text
eventId
```

Processed IDs are stored in:

```text
processed_events
```

Before processing:

```java
if (processedEventRepository.existsByEventId(event.getEventId())) {
    return;
}
```

A database unique constraint is also applied to the event ID.

This prevents:

- duplicate passenger rebooking
- duplicate seat decrement
- duplicate side effects

---

## Retry and Dead Letter Topic

Kafka consumer failures are handled using Spring Kafka's error handling.

The configuration uses:

- `DefaultErrorHandler`
- `FixedBackOff`
- `DeadLetterPublishingRecoverer`

Processing behavior:

```text
Initial attempt
     |
     X
Retry 1
     |
     X
Retry 2
     |
     X
flight.disrupted.DLT
```

The Dead Letter Topic is:

```text
flight.disrupted.DLT
```

This prevents failed disruption messages from being silently lost.

---

## Validation and Error Handling

The backend uses Bean Validation and centralized exception handling.

Examples:

```text
400 Bad Request
```

Used for invalid request data.

```text
404 Not Found
```

Used when a flight does not exist.

```text
409 Conflict
```

Used for business conflicts such as:

- booking a cancelled flight
- booking a departed flight
- attempting to reserve a sold-out flight

---

## Frontend Dashboard

The React frontend provides an airline operations dashboard.

Main pages:

```text
Dashboard
Flights
Bookings
```

### Dashboard

Displays metrics such as:

- total flights
- scheduled flights
- delayed flights
- cancelled flights
- available seats
- confirmed passengers
- rebooked passengers
- passengers requiring recovery

It also displays recent flight operations and passenger recovery activity.

### Flights

Operators can:

- view flight information
- view capacity
- view current status
- delay a flight
- cancel a flight

Cancelling or delaying a flight triggers the asynchronous Kafka recovery workflow.

### Bookings

Operators can:

- create passenger bookings
- select valid flights
- view passengers by flight
- see booking status
- see passengers automatically moved to replacement flights

---

## REST API Endpoints

### Flights

```http
POST /api/flights
```

Create a flight.

```http
GET /api/flights
```

Get all flights.

```http
GET /api/flights/{id}
```

Get one flight.

```http
GET /api/flights/search?origin=DFW&destination=ATL
```

Search flights by route.

```http
PATCH /api/flights/{id}/status
```

Update flight status.

Example:

```json
{
  "status": "CANCELLED"
}
```

### Bookings

```http
POST /api/bookings
```

Create a booking.

Example:

```json
{
  "passengerId": "PAX-5001",
  "flightId": 10
}
```

```http
GET /api/bookings
```

Get all bookings.

```http
GET /api/bookings/flight/{flightId}
```

Get bookings for a specific flight.

---

## Testing

The backend includes automated tests using JUnit and Mockito.

### BookingService tests

Tests include:

- booking succeeds when a seat is available
- booking fails when no seats remain
- booking fails for a cancelled flight

### RebookingService tests

Tests include:

- passenger is moved to the next available flight
- duplicate Kafka events are ignored
- passenger becomes `REBOOKING_REQUIRED` when no replacement is available

The application context uses an H2 in-memory database during tests so basic test execution does not depend on Docker PostgreSQL.

Run backend tests:

```bash
./gradlew clean test
```

---

## Running the Project Locally

### Prerequisites

Install:

- Java 21
- Node.js 22+
- Docker Desktop
- Git

### Start PostgreSQL and Kafka

Start Docker Desktop first.

Then start the existing containers:

```bash
docker start airline-postgres airline-kafka
```

Verify:

```bash
docker ps
```

Expected services:

```text
airline-postgres
airline-kafka
```

PostgreSQL runs on:

```text
localhost:5433
```

Kafka runs on:

```text
localhost:9092
```

---

### Start the Backend

From the project root:

```bash
./gradlew bootRun
```

Backend:

```text
http://localhost:8080
```

Health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

### Start the Frontend

From:

```text
frontend/
```

run:

```bash
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Production Frontend Build

The frontend uses Node.js 22 with Vite 8.

Run:

```bash
cd frontend
npm run build
```

The production files are generated in:

```text
frontend/dist/
```

---

## Project Structure

```text
airline-recovery-platform/
|
├── src/
│   ├── main/
│   │   ├── java/com/airline/recovery/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── event/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   |
│   │   └── resources/
│   │       └── application.properties
│   |
│   └── test/
│       ├── java/
│       └── resources/
│
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── types/
│   │   ├── App.tsx
│   │   └── index.css
│   ├── package.json
│   └── vite.config.ts
│
├── build.gradle
├── docker-compose.yml
└── README.md
```

---

## Technical Challenges and Solutions

### 1. Kafka JSON serializer compatibility

A Kafka publishing failure occurred because the original serializer expected an older Jackson class.

The issue was identified from the stack trace and resolved by using:

```java
JacksonJsonSerializer
```

compatible with the current Spring Kafka stack.

---

### 2. Duplicate Kafka delivery

Problem:

```text
Same disruption event could be delivered more than once.
```

Solution:

```text
eventId + processed_events table + unique constraint
```

This made the consumer idempotent.

---

### 3. Concurrent last-seat booking

Problem:

```text
Two requests could read the same final available seat.
```

Solution:

```text
Atomic conditional SQL update
```

This prevents overselling.

---

### 4. Kafka consumer failures

Problem:

```text
A failed consumer operation should not lose the event.
```

Solution:

```text
Retry + FixedBackOff + Dead Letter Topic
```

---

### 5. Database and Kafka dual-write risk

During development, a failure demonstrated that the database update could succeed while Kafka publishing failed.

For a production version, the next improvement would be the:

```text
Transactional Outbox Pattern
```

The flight update and outbox event would be committed in the same database transaction, and a separate publisher would reliably send the event to Kafka.

---

### 6. External dependencies during tests

The original Spring context test depended on PostgreSQL running locally.

The test environment was separated using:

```text
H2 in-memory database
```

This makes basic automated tests faster and independent of local Docker infrastructure.

---

## Future Improvements

Potential production enhancements include:

- transactional outbox pattern
- Spring Security with OAuth2 / JWT
- role-based access control
- Testcontainers integration tests
- schema migrations with Flyway or Liquibase
- Prometheus / Grafana metrics
- distributed tracing
- structured logging
- passenger notification service
- email/SMS notifications
- flight inventory service separation
- Kubernetes deployment
- AWS deployment
- GitHub Actions CI/CD
- centralized Kafka schema management

---

## Interview Summary

A concise explanation of the project:

> I built a full-stack Airline Recovery Platform using Java 21, Spring Boot, React, TypeScript, PostgreSQL, Kafka, and Docker. The application manages flights and passenger bookings. When a flight is cancelled or delayed, Spring Boot publishes a Kafka disruption event. A Kafka consumer asynchronously finds affected passengers, searches for eligible later flights, atomically reserves replacement capacity, and updates passenger bookings to REBOOKED. I also added idempotent event processing, Kafka retries and a dead-letter topic, concurrency-safe seat inventory, validation, centralized exception handling, and automated JUnit/Mockito tests. The React dashboard allows operators to monitor flights, create bookings, trigger disruptions, and see passenger recovery results.

---

## Key Engineering Concepts Demonstrated

This project demonstrates practical experience with:

- REST API design
- layered Spring Boot architecture
- relational data modeling
- transactional consistency
- event-driven architecture
- Kafka producers and consumers
- asynchronous workflows
- idempotent consumers
- dead-letter queues/topics
- retry strategies
- concurrency control
- atomic database operations
- React API integration
- TypeScript
- Docker-based infrastructure
- unit testing and mocking
- production failure analysis
