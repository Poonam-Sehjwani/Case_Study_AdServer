
package com.retail.streaming.services;

import org.springframework.stereotype.Service;

import com.retail.streaming.dto.RetailEventRequest;

// just drops the event onto kafka so ingestion stays fast and processing happens later
@Service
public class EventIngestionService {

    private final EventProducerService eventProducerService;

    public EventIngestionService(
            EventProducerService eventProducerService) {
        this.eventProducerService = eventProducerService;
    }

    public String ingest(RetailEventRequest event) {

        eventProducerService.publish(event);

        return "Event " + event.eventId()
                + " submitted to Kafka";
    }
}
