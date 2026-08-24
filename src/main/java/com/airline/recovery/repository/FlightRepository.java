package com.airline.recovery.repository;

import com.airline.recovery.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginIgnoreCaseAndDestinationIgnoreCase(
            String origin,
            String destination);
}