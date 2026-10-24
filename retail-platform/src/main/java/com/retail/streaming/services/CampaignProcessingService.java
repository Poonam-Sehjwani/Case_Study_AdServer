
package com.retail.streaming.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.retail.streaming.dto.CampaignMetricsResponse;
import com.retail.streaming.dto.RetailEventRequest;
import com.retail.streaming.entity.CampaignMetric;
import com.retail.streaming.entity.ClickSession;
import com.retail.streaming.entity.ProcessedEvent;
import com.retail.streaming.repository.CampaignMetricRepository;
import com.retail.streaming.repository.ClickSessionRepository;
import com.retail.streaming.repository.ProcessedEventRepository;

// takes one event and saves it + updates the campaign counts
// handles duplicates and two events updating the same counter at once
@Service
public class CampaignProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(CampaignProcessingService.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration ATTRIBUTION_WINDOW =
            Duration.ofMinutes(30);

    private final ProcessedEventRepository processedEventRepository;
    private final CampaignMetricRepository campaignMetricRepository;
    private final ClickSessionRepository clickSessionRepository;
    private final TransactionTemplate transactionTemplate;

    public CampaignProcessingService(
            ProcessedEventRepository processedEventRepository,
            CampaignMetricRepository campaignMetricRepository,
            ClickSessionRepository clickSessionRepository,
            PlatformTransactionManager transactionManager) {

        this.processedEventRepository = processedEventRepository;
        this.campaignMetricRepository = campaignMetricRepository;
        this.clickSessionRepository = clickSessionRepository;

        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // each try runs in its own transaction so a failed one rolls back clean
    // retry only on clashes we can safely redo (same counter / duplicate)
    // give up after 3 tries -> goes to the dead letter topic
    public void process(RetailEventRequest event) {

        String eventType = event.eventType().toString();
        Instant eventTime = event.eventTime();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Boolean processed = transactionTemplate.execute(
                        status -> processInTransaction(
                                event, eventType, eventTime));

                if (Boolean.FALSE.equals(processed)) {
                    log.info("Duplicate event ignored: retailerId={}, eventId={}",
                            event.retailerId(), event.eventId());
                } else {
                    log.info("Event processed: retailerId={}, eventId={}",
                            event.retailerId(), event.eventId());
                }

                return;

            } catch (OptimisticLockingFailureException
                     | DataIntegrityViolationException ex) {

                if (attempt == MAX_ATTEMPTS) {
                    log.error(
                            "Processing failed after {} attempts: retailerId={}, eventId={}",
                            MAX_ATTEMPTS, event.retailerId(),
                            event.eventId(), ex);
                    throw ex;
                }

                log.warn(
                        "Retrying event, attempt {}: retailerId={}, eventId={}",
                        attempt + 1, event.retailerId(), event.eventId());
            }
        }
    }

    private boolean processInTransaction(
            RetailEventRequest event,
            String eventType,
            Instant eventTime) {

        // already saved this event for this retailer? skip it
        if (processedEventRepository.existsByRetailerIdAndEventId(
                event.retailerId(), event.eventId())) {
            return false;
        }

        ProcessedEvent processedEvent = new ProcessedEvent(
                event.eventId(),
                eventType,
                eventTime,
                event.retailerId(),
                event.sessionId(),
                event.anonymousCustomerId(),
                event.productId(),
                event.campaignId(),
                event.adId());

        // save now so a duplicate blows up here where we can catch it, not later
        processedEventRepository.saveAndFlush(processedEvent);

        // click: remember it so a basket added later in the same session links back
        if ("AD_CLICK".equals(eventType)) {
            if (hasText(event.campaignId())
                    && hasText(event.sessionId())
                    && eventTime != null) {

                ClickSession clickSession = new ClickSession(
                        event.retailerId(),
                        event.sessionId(),
                        event.anonymousCustomerId(),
                        event.campaignId(),
                        event.eventId(),
                        eventTime);

                clickSessionRepository.saveAndFlush(clickSession);
            }

            // Count the click even when session information is unavailable.
            if (hasText(event.campaignId())) {
                updateMetric(
                        event.retailerId(),
                        event.campaignId(),
                        new EventCounts(0, 1, 0, 0));
            }

            return true;
        }

        // basket add: find a click in the same session in the last 30 mins
        // if found, link it and give credit to that click's campaign
        // note: if the basket comes in before its click, we miss it (undercount)
        if ("ADD_TO_BASKET".equals(eventType)) {
            if (!hasText(event.sessionId()) || eventTime == null) {
                return true;
            }

            Instant windowStart = eventTime.minus(ATTRIBUTION_WINDOW);

            List<ClickSession> eligibleClicks =
                    clickSessionRepository
                            .findByRetailerIdAndSessionIdAndAttributedAtIsNullAndClickedAtBetweenOrderByClickedAtDesc(
                                    event.retailerId(),
                                    event.sessionId(),
                                    windowStart,
                                    eventTime);

            ClickSession matchingClick = eligibleClicks.stream()
                    .filter(click ->
                            !hasText(event.anonymousCustomerId())
                            || !hasText(click.getAnonymousCustomerId())
                            || event.anonymousCustomerId().equals(
                                    click.getAnonymousCustomerId()))
                    .findFirst()
                    .orElse(null);

            if (matchingClick != null) {
                matchingClick.markAttributed(eventTime);
                clickSessionRepository.saveAndFlush(matchingClick);

                // Credit the campaign that was clicked, not an arbitrary
                // campaign supplied with the basket event.
                updateMetric(
                        event.retailerId(),
                        matchingClick.getCampaignId(),
                        new EventCounts(0, 0, 1, 0));

                log.info(
                        "Basket attributed: retailerId={}, campaignId={}, clickEventId={}",
                        event.retailerId(),
                        matchingClick.getCampaignId(),
                        matchingClick.getClickEventId());
            }

            // No matching click means no attributed basket addition.
            return true;
        }

        // Events without a campaign are stored as raw events only.
        if (!hasText(event.campaignId())) {
            return true;
        }

        EventCounts counts = getEventCounts(eventType);

        updateMetric(event.retailerId(), event.campaignId(), counts);

        return true;
    }

    private void updateMetric(
            String retailerId,
            String campaignId,
            EventCounts counts) {

        // read count, add to it, save. version field stops two writers clobbering each other
        CampaignMetric metric = campaignMetricRepository
                .findByRetailerIdAndCampaignId(retailerId, campaignId)
                .orElseGet(() -> new CampaignMetric(retailerId, campaignId));

        metric.increment(
                counts.impressions(),
                counts.clicks(),
                counts.basketAdditions(),
                counts.productViews());

        campaignMetricRepository.saveAndFlush(metric);
    }

    private EventCounts getEventCounts(String eventType) {
        return switch (eventType) {
            case "AD_IMPRESSION" ->
                    new EventCounts(1, 0, 0, 0);

            case "PRODUCT_VIEW" ->
                    new EventCounts(0, 0, 0, 1);

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + eventType);
        };
    }

    public CampaignMetricsResponse getMetrics(
            String retailerId, String campaignId) {

        CampaignMetric metric = campaignMetricRepository
                .findByRetailerIdAndCampaignId(retailerId, campaignId)
                .orElse(null);

        if (metric == null) {
            return new CampaignMetricsResponse(
                    campaignId, 0, 0, 0, 0);
        }

        return new CampaignMetricsResponse(
                metric.getCampaignId(),
                metric.getImpressions(),
                metric.getClicks(),
                metric.getBasketAdditions(),
                metric.getProductViews());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record EventCounts(
            long impressions,
            long clicks,
            long basketAdditions,
            long productViews) {
    }
}
