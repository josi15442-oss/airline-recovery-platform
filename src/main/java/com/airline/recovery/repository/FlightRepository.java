package com.airline.recovery.repository;

import com.airline.recovery.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginIgnoreCaseAndDestinationIgnoreCase(
            String origin,
            String destination
    );

   @Modifying(
    flushAutomatically = true,
    clearAutomatically = true
)
    @Query(value = """
            UPDATE flights
            SET available_seats = available_seats - 1
            WHERE id = :flightId
              AND available_seats > 0
            """,
            nativeQuery = true)
    int decrementSeatIfAvailable(@Param("flightId") Long flightId);
}