package com.airline.recovery.repository;

import com.airline.recovery.entity.Booking;
import com.airline.recovery.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByFlight_Id(Long flightId);

    List<Booking> findByFlight_IdAndStatus(
            Long flightId,
            BookingStatus status
    );
}