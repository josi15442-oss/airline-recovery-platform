package com.airline.recovery.service;

import com.airline.recovery.entity.Flight;
import com.airline.recovery.entity.FlightStatus;
import com.airline.recovery.exception.FlightNotFoundException;
import com.airline.recovery.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
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

    return flightRepository.save(flight);
}
}