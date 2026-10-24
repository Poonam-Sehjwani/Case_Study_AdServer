
package com.retail.streaming.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "click_sessions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_click_sessions_tenant_event",
            columnNames = {"retailer_id", "click_event_id"}
        )
    },
    indexes = {
        @Index(
            name = "idx_click_sessions_attribution",
            columnList = "retailer_id, session_id, campaign_id, clicked_at"
        )
    }
)
public class ClickSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false, length = 100)
    private String retailerId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "anonymous_customer_id", length = 100)
    private String anonymousCustomerId;

    @Column(name = "campaign_id", nullable = false, length = 100)
    private String campaignId;

    @Column(name = "click_event_id", nullable = false, length = 100)
    private String clickEventId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "attributed_at")
    private Instant attributedAt;

    protected ClickSession() {
    }

    public ClickSession(
            String retailerId,
            String sessionId,
            String anonymousCustomerId,
            String campaignId,
            String clickEventId,
            Instant clickedAt) {

        this.retailerId = retailerId;
        this.sessionId = sessionId;
        this.anonymousCustomerId = anonymousCustomerId;
        this.campaignId = campaignId;
        this.clickEventId = clickEventId;
        this.clickedAt = clickedAt;
    }

    public void markAttributed(Instant attributedAt) {
        this.attributedAt = attributedAt;
    }

    public Long getId() {
        return id;
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

    public String getCampaignId() {
        return campaignId;
    }

    public String getClickEventId() {
        return clickEventId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public Instant getAttributedAt() {
        return attributedAt;
    }
}
