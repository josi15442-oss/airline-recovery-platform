package com.airline.recovery.event;

import com.airline.recovery.entity.FlightStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class FlightDisruptedEvent {

    private String eventId;
    private Long flightId;
    private String flightNumber;
    private String origin;
    private String destination;
    private FlightStatus status;
    private LocalDateTime occurredAt;

    public FlightDisruptedEvent() {
    }

    public FlightDisruptedEvent(
            Long flightId,
            String flightNumber,
            String origin,
            String destination,
            FlightStatus status) {

        this.eventId = UUID.randomUUID().toString();
        this.flightId = flightId;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.status = status;
        this.occurredAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Long getFlightId() {
        return flightId;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}