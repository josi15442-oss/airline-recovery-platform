package com.airline.recovery.service;

import com.airline.recovery.entity.Flight;
import com.airline.recovery.entity.FlightStatus;
import com.airline.recovery.exception.FlightNotFoundException;
import com.airline.recovery.repository.FlightRepository;
import org.springframework.stereotype.Service;
import com.airline.recovery.event.FlightDisruptedEvent;
import com.airline.recovery.event.FlightEventPublisher;
import java.util.List;


@Service
public class FlightService {

   private final FlightRepository flightRepository;
private final FlightEventPublisher flightEventPublisher;

   public FlightService(
        FlightRepository flightRepository,
        FlightEventPublisher flightEventPublisher) {

    this.flightRepository = flightRepository;
    this.flightEventPublisher = flightEventPublisher;
}

    public Flight createFlight(Flight flight) {
        return flightRepository.save(flight);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

   public Flight getFlightById(Long id) {
    return flightRepository.findById(id)
            .orElseThrow(() -> new FlightNotFoundException(id));
}

    public List<Flight> searchFlights(String origin, String destination) {

        return flightRepository
                .findByOriginIgnoreCaseAndDestinationIgnoreCase(
                        origin,
                        destination);
    }

public Flight updateFlightStatus(
        Long id,
        FlightStatus newStatus) {

    Flight flight = flightRepository.findById(id)
            .orElseThrow(() -> new FlightNotFoundException(id));

    flight.setStatus(newStatus);

    Flight updatedFlight = flightRepository.save(flight);

    if (newStatus == FlightStatus.CANCELLED ||
            newStatus == FlightStatus.DELAYED) {

        FlightDisruptedEvent event =
                new FlightDisruptedEvent(
                        updatedFlight.getId(),
                        updatedFlight.getFlightNumber(),
                        updatedFlight.getOrigin(),
                        updatedFlight.getDestination(),
                        updatedFlight.getStatus()
                );

        flightEventPublisher.publishFlightDisrupted(event);
    }

    return updatedFlight;
}
}