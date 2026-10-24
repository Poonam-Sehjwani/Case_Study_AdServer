
package com.retail.streaming.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.retail.streaming.dto.RetailEventRequest;

// reads events off kafka and passes them to the processor
// if it keeps failing it ends up in the DLT
@Service
public class EventConsumerService {

    private static final Logger log =
            LoggerFactory.getLogger(EventConsumerService.class);

    private final CampaignProcessingService campaignProcessingService;

    public EventConsumerService(
    		CampaignProcessingService campaignProcessingService) {
        this.campaignProcessingService = campaignProcessingService;
    }

    @KafkaListener(
            topics = "retail-events",
            groupId = "retail-event-processors"
    )
    public void consume(RetailEventRequest event) {
        log.info("Processing eventId={}, eventType={}",
                event.eventId(), event.eventType());
        
		/*
		 * // TEMPORARY: Test Kafka retry and Dead Letter Topic (DLT) if
		 * ("dlt-test".equals(event.eventId())) { throw new
		 * RuntimeException("Intentional DLT test failure"); }
		 */
        campaignProcessingService.process(event);
    }
}
