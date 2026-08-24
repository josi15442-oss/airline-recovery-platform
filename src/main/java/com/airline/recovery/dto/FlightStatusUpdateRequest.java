package com.airline.recovery.dto;

import com.airline.recovery.entity.FlightStatus;
import jakarta.validation.constraints.NotNull;

public class FlightStatusUpdateRequest {

    @NotNull(message = "Flight status is required")
    private FlightStatus status;

    public FlightStatusUpdateRequest() {
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }
}