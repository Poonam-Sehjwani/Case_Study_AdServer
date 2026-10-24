
package com.retail.streaming.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import com.retail.streaming.dto.EventType;
import com.retail.streaming.dto.RetailEventRequest;
import com.retail.streaming.entity.CampaignMetric;
import com.retail.streaming.entity.ClickSession;
import com.retail.streaming.repository.CampaignMetricRepository;
import com.retail.streaming.repository.ClickSessionRepository;
import com.retail.streaming.repository.ProcessedEventRepository;

class CampaignProcessingServiceTest {

    private ProcessedEventRepository processedEventRepository;
    private CampaignMetricRepository campaignMetricRepository;
    private ClickSessionRepository clickSessionRepository;
    private PlatformTransactionManager transactionManager;

    private CampaignProcessingService service;

    @BeforeEach
    void setUp() {
        processedEventRepository = mock(ProcessedEventRepository.class);
        campaignMetricRepository = mock(CampaignMetricRepository.class);
        clickSessionRepository = mock(ClickSessionRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);

        service = new CampaignProcessingService(
                processedEventRepository,
                campaignMetricRepository,
                clickSessionRepository,
                transactionManager
        );
    }

    @Test
    void duplicateEventShouldNotUpdateMetrics() {
        RetailEventRequest event = new RetailEventRequest(
                "click-1001",
                com.retail.streaming.dto.EventType.AD_CLICK,
                Instant.parse("2026-10-23T10:00:00Z"),
                "retailer-A",
                "session-1",
                "product-1",
                "campaign-101",
                "ad-1",
                "customer-1"
        );

        when(processedEventRepository.existsByRetailerIdAndEventId(
                "retailer-A", "click-1001"
        )).thenReturn(true);

        service.process(event);

        verify(processedEventRepository).existsByRetailerIdAndEventId(
                "retailer-A", "click-1001"
        );

        verify(processedEventRepository, never()).saveAndFlush(any());
        verify(campaignMetricRepository, never())
                .saveAndFlush(any(CampaignMetric.class));
        verify(clickSessionRepository, never()).saveAndFlush(any());
    }
    

	@Test
	void metricsShouldBeIsolatedByRetailer() {
	    CampaignMetric retailerAMetric =
	            new CampaignMetric("retailer-A", "campaign-101");
	
	    retailerAMetric.increment(3, 5, 1, 2);
	
	    when(campaignMetricRepository.findByRetailerIdAndCampaignId(
	            "retailer-A", "campaign-101"))
	            .thenReturn(Optional.of(retailerAMetric));
	
	    var result = service.getMetrics("retailer-A", "campaign-101");
	
	    org.junit.jupiter.api.Assertions.assertEquals(
	            "campaign-101", result.campaignId());
	    org.junit.jupiter.api.Assertions.assertEquals(3, result.impressions());
	    org.junit.jupiter.api.Assertions.assertEquals(5, result.clicks());
	    org.junit.jupiter.api.Assertions.assertEquals(1, result.basketadditions());
	    org.junit.jupiter.api.Assertions.assertEquals(2, result.productviews());
	
	    verify(campaignMetricRepository).findByRetailerIdAndCampaignId(
	            "retailer-A", "campaign-101");
	}

	@Test
	void basketWithin30MinutesShouldBeAttributedToClick() {
	    Instant clickTime = Instant.parse("2026-10-23T10:00:00Z");
	    Instant basketTime = clickTime.plusSeconds(5 * 60);

	    ClickSession click = new ClickSession(
	            "retailer-A",
	            "session-1",
	            "customer-1",
	            "campaign-101",
	            "click-1001",
	            clickTime
	    );

	    when(processedEventRepository.existsByRetailerIdAndEventId(
	            "retailer-A", "basket-1001")).thenReturn(false);

	    when(clickSessionRepository
	            .findByRetailerIdAndSessionIdAndAttributedAtIsNullAndClickedAtBetweenOrderByClickedAtDesc(
	                    eq("retailer-A"),
	                    eq("session-1"),
	                    eq(basketTime.minusSeconds(30 * 60)),
	                    eq(basketTime)))
	            .thenReturn(List.of(click));

	    when(campaignMetricRepository.findByRetailerIdAndCampaignId(
	            "retailer-A", "campaign-101"))
	            .thenReturn(Optional.of(
	                    new CampaignMetric("retailer-A", "campaign-101")));

	    RetailEventRequest basketEvent = new RetailEventRequest(
	            "basket-1001",
	            EventType.ADD_TO_BASKET,
	            basketTime,
	            "retailer-A",
	            "session-1",
	            "product-1",
	            "campaign-101",
	            null,
	            "customer-1"
	    );

	    service.process(basketEvent);

	    assertEquals(basketTime, click.getAttributedAt());

	    verify(clickSessionRepository).saveAndFlush(click);
	    verify(campaignMetricRepository).saveAndFlush(any(CampaignMetric.class));
	}

	@Test
	void basketAfter30MinutesShouldNotBeAttributed() {
	    Instant clickTime = Instant.parse("2026-10-23T10:00:00Z");
	    Instant basketTime = clickTime.plusSeconds(35 * 60);

	    when(processedEventRepository.existsByRetailerIdAndEventId(
	            "retailer-A", "basket-1002")).thenReturn(false);

	    when(clickSessionRepository
	            .findByRetailerIdAndSessionIdAndAttributedAtIsNullAndClickedAtBetweenOrderByClickedAtDesc(
	                    eq("retailer-A"),
	                    eq("session-1"),
	                    eq(basketTime.minusSeconds(30 * 60)),
	                    eq(basketTime)))
	            .thenReturn(List.of());

	    RetailEventRequest basketEvent = new RetailEventRequest(
	            "basket-1002",
	            EventType.ADD_TO_BASKET,
	            basketTime,
	            "retailer-A",
	            "session-1",
	            "product-1",
	            "campaign-101",
	            null,
	            "customer-1"
	    );

	    service.process(basketEvent);

	    verify(clickSessionRepository, never()).saveAndFlush(any(ClickSession.class));
	    verify(campaignMetricRepository, never()).saveAndFlush(any(CampaignMetric.class));
	}

	@Test
	void basketFromDifferentCustomerShouldNotBeAttributed() {
	    Instant clickTime = Instant.parse("2026-10-23T10:00:00Z");
	    Instant basketTime = clickTime.plusSeconds(5 * 60);

	    ClickSession click = new ClickSession(
	            "retailer-A",
	            "session-1",
	            "customer-1",
	            "campaign-101",
	            "click-1003",
	            clickTime
	    );

	    when(processedEventRepository.existsByRetailerIdAndEventId(
	            "retailer-A", "basket-1003")).thenReturn(false);

	    when(clickSessionRepository
	            .findByRetailerIdAndSessionIdAndAttributedAtIsNullAndClickedAtBetweenOrderByClickedAtDesc(
	                    eq("retailer-A"),
	                    eq("session-1"),
	                    eq(basketTime.minusSeconds(30 * 60)),
	                    eq(basketTime)))
	            .thenReturn(List.of(click));

	    RetailEventRequest basketEvent = new RetailEventRequest(
	            "basket-1003",
	            EventType.ADD_TO_BASKET,
	            basketTime,
	            "retailer-A",
	            "session-1",
	            "product-1",
	            "campaign-101",
	            null,
	            "customer-2"
	    );

	    service.process(basketEvent);

	    assertEquals(null, click.getAttributedAt());

	    verify(clickSessionRepository, never()).saveAndFlush(any(ClickSession.class));
	    verify(campaignMetricRepository, never()).saveAndFlush(any(CampaignMetric.class));
	}
}
