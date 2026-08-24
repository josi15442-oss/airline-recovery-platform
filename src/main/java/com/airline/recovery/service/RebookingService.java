package com.airline.recovery.service;



import com.airline.recovery.entity.Booking;
import com.airline.recovery.entity.BookingStatus;
import com.airline.recovery.entity.Flight;
import com.airline.recovery.entity.FlightStatus;
import com.airline.recovery.entity.ProcessedEvent;

import com.airline.recovery.event.FlightDisruptedEvent;
import com.airline.recovery.exception.FlightNotFoundException;

import com.airline.recovery.repository.BookingRepository;
import com.airline.recovery.repository.FlightRepository;
import com.airline.recovery.repository.ProcessedEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class RebookingService {

    private static final Logger logger =
            LoggerFactory.getLogger(RebookingService.class);

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final ProcessedEventRepository processedEventRepository;

    public RebookingService(
            BookingRepository bookingRepository,
            FlightRepository flightRepository,
            ProcessedEventRepository processedEventRepository) {

        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void handleFlightDisruption(FlightDisruptedEvent event) {

        // 1. Protect against duplicate Kafka events
        if (processedEventRepository.existsByEventId(event.getEventId())) {

            logger.info(
                    "Skipping duplicate disruption event eventId={}",
                    event.getEventId()
            );

            return;
        }

        // 2. Find the disrupted flight
        Flight disruptedFlight =
                flightRepository.findById(event.getFlightId())
                        .orElseThrow(() ->
                                new FlightNotFoundException(
                                        event.getFlightId()
                                ));

        // 3. Find confirmed bookings affected by the disruption
        List<Booking> affectedBookings =
                bookingRepository.findByFlight_IdAndStatus(
                        disruptedFlight.getId(),
                        BookingStatus.CONFIRMED
                );

        // 4. Even if there are no bookings, mark the event as processed
        if (affectedBookings.isEmpty()) {

            processedEventRepository.save(
                    new ProcessedEvent(
                            event.getEventId(),
                            LocalDateTime.now()
                    )
            );

            logger.info(
                    "No confirmed bookings for eventId={}; event marked processed",
                    event.getEventId()
            );

            return;
        }

        // 5. Find alternative flights for the same route
        List<Flight> alternatives =
                flightRepository
                        .findByOriginIgnoreCaseAndDestinationIgnoreCase(
                                disruptedFlight.getOrigin(),
                                disruptedFlight.getDestination()
                        )
                        .stream()

                        // Do not select the disrupted flight itself
                        .filter(flight ->
                                !flight.getId()
                                        .equals(disruptedFlight.getId()))

                        // Only usable flights
                        .filter(flight ->
                                flight.getStatus() == FlightStatus.SCHEDULED
                                        ||
                                flight.getStatus() == FlightStatus.ON_TIME)

                        // Must have available seats
                        .filter(flight ->
                                flight.getAvailableSeats() > 0)

                        // Must depart after the disrupted flight
                        .filter(flight ->
                                flight.getDepartureTime()
                                        .isAfter(
                                                disruptedFlight
                                                        .getDepartureTime()
                                        ))

                        // Earliest available flight first
                        .sorted(
                                Comparator.comparing(
                                        Flight::getDepartureTime
                                )
                        )
                        .toList();

        // 6. Rebook each affected passenger
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

            // No alternative found
            if (alternative == null) {

                bookingRepository.save(booking);

                logger.info(
                        "No alternative flight available for passenger {}",
                        booking.getPassengerId()
                );

                continue;
            }

            // Reduce seat inventory on replacement flight
            alternative.setAvailableSeats(
                    alternative.getAvailableSeats() - 1
            );

            flightRepository.save(alternative);

            // Move booking to replacement flight
            booking.setFlight(alternative);
            booking.setStatus(
                    BookingStatus.REBOOKED
            );

            bookingRepository.save(booking);

            logger.info(
                    "Passenger {} rebooked from {} to {}",
                    booking.getPassengerId(),
                    disruptedFlight.getFlightNumber(),
                    alternative.getFlightNumber()
            );
        }

        // 7. Mark Kafka event as successfully processed
        processedEventRepository.save(
                new ProcessedEvent(
                        event.getEventId(),
                        LocalDateTime.now()
                )
        );

        logger.info(
                "Disruption event processed successfully eventId={}",
                event.getEventId()
        );
    }
}