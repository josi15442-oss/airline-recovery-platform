package com.airline.recovery.event;

import com.airline.recovery.service.RebookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FlightDisruptionConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(FlightDisruptionConsumer.class);

    private final RebookingService rebookingService;

    public FlightDisruptionConsumer(
            RebookingService rebookingService) {
        this.rebookingService = rebookingService;
    }

    @KafkaListener(
            topics = "flight.disrupted",
            groupId = "airline-recovery-group"
    )
    public void handleFlightDisruption(
            FlightDisruptedEvent event) {

        logger.info(
                ">>> RECEIVED KAFKA EVENT: flightId={}, flightNumber={}, status={}",
                event.getFlightId(),
                event.getFlightNumber(),
                event.getStatus()
        );

        rebookingService.handleFlightDisruption(event);

        logger.info(
                ">>> REBOOKING FINISHED FOR {}",
                event.getFlightNumber()
        );
    }
}