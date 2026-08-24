package com.airline.recovery.service;

import com.airline.recovery.dto.BookingRequest;
import com.airline.recovery.entity.Booking;
import com.airline.recovery.entity.BookingStatus;
import com.airline.recovery.entity.Flight;
import com.airline.recovery.entity.FlightStatus;
import com.airline.recovery.exception.BookingConflictException;
import com.airline.recovery.exception.FlightNotFoundException;
import com.airline.recovery.repository.BookingRepository;
import com.airline.recovery.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    public BookingService(
            BookingRepository bookingRepository,
            FlightRepository flightRepository) {

        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() ->
                        new FlightNotFoundException(request.getFlightId()));

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new BookingConflictException(
                    "Cannot book a cancelled flight"
            );
        }

        if (flight.getStatus() == FlightStatus.DEPARTED ||
                flight.getStatus() == FlightStatus.ARRIVED) {

            throw new BookingConflictException(
                    "Cannot book a flight that has already departed"
            );
        }

        if (flight.getAvailableSeats() <= 0) {
            throw new BookingConflictException(
                    "No seats available for flight " +
                            flight.getFlightNumber()
            );
        }

        flight.setAvailableSeats(
                flight.getAvailableSeats() - 1
        );

        flightRepository.save(flight);

        Booking booking = new Booking(
                request.getPassengerId(),
                flight,
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByFlight(Long flightId) {

        return bookingRepository.findByFlight_Id(flightId);
    }
}