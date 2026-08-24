package com.airline.recovery.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FlightEventPublisher {

    private static final String FLIGHT_DISRUPTED_TOPIC =
            "flight.disrupted";

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public FlightEventPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate) {

        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishFlightDisrupted(
            FlightDisruptedEvent event) {

        kafkaTemplate.send(
                FLIGHT_DISRUPTED_TOPIC,
                event.getFlightId().toString(),
                event
        );
    }
}