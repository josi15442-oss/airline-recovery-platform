package com.airline.recovery.service;

import com.airline.recovery.entity.Booking;
import com.airline.recovery.entity.BookingStatus;
import com.airline.recovery.entity.Flight;
import com.airline.recovery.entity.FlightStatus;
import com.airline.recovery.event.FlightDisruptedEvent;
import com.airline.recovery.exception.FlightNotFoundException;
import com.airline.recovery.repository.BookingRepository;
import com.airline.recovery.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class RebookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;

    public RebookingService(
            BookingRepository bookingRepository,
            FlightRepository flightRepository) {

        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    @Transactional
    public void handleFlightDisruption(
            FlightDisruptedEvent event) {

        Flight disruptedFlight =
                flightRepository.findById(event.getFlightId())
                        .orElseThrow(() ->
                                new FlightNotFoundException(
                                        event.getFlightId()
                                ));

        List<Booking> affectedBookings =
                bookingRepository.findByFlight_IdAndStatus(
                        disruptedFlight.getId(),
                        BookingStatus.CONFIRMED
                );

        if (affectedBookings.isEmpty()) {
            return;
        }

        List<Flight> alternatives =
                flightRepository
                        .findByOriginIgnoreCaseAndDestinationIgnoreCase(
                                disruptedFlight.getOrigin(),
                                disruptedFlight.getDestination()
                        )
                        .stream()
                        .filter(flight ->
                                !flight.getId()
                                        .equals(disruptedFlight.getId()))
                        .filter(flight ->
                                flight.getStatus()
                                        == FlightStatus.SCHEDULED
                                ||
                                flight.getStatus()
                                        == FlightStatus.ON_TIME)
                        .filter(flight ->
                                flight.getAvailableSeats() > 0)
                        .filter(flight ->
                                flight.getDepartureTime()
                                        .isAfter(
                                                disruptedFlight
                                                        .getDepartureTime()
                                        ))
                        .sorted(
                                Comparator.comparing(
                                        Flight::getDepartureTime
                                )
                        )
                        .toList();

        for (Booking booking : affectedBookings) {

            booking.setStatus(
                    BookingStatus.REBOOKING_REQUIRED
            );

            Flight alternative =
                    alternatives.stream()
                            .filter(flight ->
                                    flight.getAvailableSeats() > 0)
                            .findFirst()
                            .orElse(null);

            if (alternative == null) {
                bookingRepository.save(booking);
                continue;
            }

            alternative.setAvailableSeats(
                    alternative.getAvailableSeats() - 1
            );

            flightRepository.save(alternative);

            booking.setFlight(alternative);
            booking.setStatus(
                    BookingStatus.REBOOKED
            );

            bookingRepository.save(booking);
        }
    }
}