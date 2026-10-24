
package com.retail.streaming.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "processed_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_processed_events_tenant_event",
            columnNames = {"retailer_id", "event_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_processed_events_tenant_campaign_time",
            columnList = "retailer_id, campaign_id, event_time"
        )
    }
)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_time")
    private Instant eventTime;

    @Column(name = "retailer_id", nullable = false, length = 100)
    private String retailerId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "anonymous_customer_id", length = 100)
    private String anonymousCustomerId;

    @Column(name = "product_id", length = 100)
    private String productId;

    @Column(name = "campaign_id", length = 100)
    private String campaignId;

    @Column(name = "ad_id", length = 100)
    private String adId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    public ProcessedEvent(
            String eventId,
            String eventType,
            Instant eventTime,
            String retailerId,
            String sessionId,
            String anonymousCustomerId,
            String productId,
            String campaignId,
            String adId) {

        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.retailerId = retailerId;
        this.sessionId = sessionId;
        this.anonymousCustomerId = anonymousCustomerId;
        this.productId = productId;
        this.campaignId = campaignId;
        this.adId = adId;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getRetailerId() {
        return retailerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAnonymousCustomerId() {
        return anonymousCustomerId;
    }

    public String getProductId() {
        return productId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public String getAdId() {
        return adId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
