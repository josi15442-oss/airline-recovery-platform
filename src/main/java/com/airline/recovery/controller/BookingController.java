package com.airline.recovery.controller;

import com.airline.recovery.dto.BookingRequest;
import com.airline.recovery.entity.Booking;
import com.airline.recovery.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequest request) {

        Booking booking = bookingService.createBooking(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(booking);
    }

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Booking>> getBookingsByFlight(
            @PathVariable Long flightId) {

        return ResponseEntity.ok(
                bookingService.getBookingsByFlight(flightId)
        );
    }
}