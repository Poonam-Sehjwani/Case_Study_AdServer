package com.retail.streaming.services;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.retail.streaming.dto.RetailEventRequest;

@Service
public class EventProducerService {

    private static final String TOPIC = "retail-events";

    private final KafkaTemplate<String, RetailEventRequest> kafkaTemplate;

    public EventProducerService(
            KafkaTemplate<String, RetailEventRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RetailEventRequest event) {
        kafkaTemplate.send(TOPIC, event);
    }
}
