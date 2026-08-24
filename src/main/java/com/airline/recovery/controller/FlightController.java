package com.airline.recovery.controller;

import com.airline.recovery.dto.FlightStatusUpdateRequest;
import com.airline.recovery.entity.Flight;
import com.airline.recovery.service.FlightService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

@PostMapping
public ResponseEntity<Flight> createFlight(
        @Valid @RequestBody Flight flight) {

    Flight createdFlight = flightService.createFlight(flight);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdFlight);
}

    @GetMapping
    public ResponseEntity<List<Flight>> getAllFlights() {

        return ResponseEntity.ok(
                flightService.getAllFlights());
    }

 @GetMapping("/{id}")
public ResponseEntity<Flight> getFlightById(@PathVariable Long id) {

    return ResponseEntity.ok(
            flightService.getFlightById(id)
    );
}

    @GetMapping("/search")
    public ResponseEntity<List<Flight>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination) {

        return ResponseEntity.ok(
                flightService.searchFlights(origin, destination));
    }

@PatchMapping("/{id}/status")
public ResponseEntity<Flight> updateFlightStatus(
        @PathVariable Long id,
        @Valid @RequestBody FlightStatusUpdateRequest request) {

    return ResponseEntity.ok(
            flightService.updateFlightStatus(
                    id,
                    request.getStatus()
            )
    );
}
}