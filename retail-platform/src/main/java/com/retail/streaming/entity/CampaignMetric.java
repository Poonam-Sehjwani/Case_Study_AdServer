
package com.retail.streaming.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "campaign_metrics",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_campaign_metrics_tenant_campaign",
            columnNames = {"retailer_id", "campaign_id"}
        )
    }
)
public class CampaignMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false, length = 100)
    private String retailerId;

    @Column(name = "campaign_id", nullable = false, length = 100)
    private String campaignId;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long clicks;

    @Column(name = "basket_additions", nullable = false)
    private long basketAdditions;

    @Column(name = "product_views", nullable = false)
    private long productViews;

    //stops two updates on the same campaign from overwriting each other
    // stops two updates on the same campaign from overwriting each other
    @Version
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CampaignMetric() {
    }

    public CampaignMetric(String retailerId, String campaignId) {
        this.retailerId = retailerId;
        this.campaignId = campaignId;
        this.updatedAt = Instant.now();
    }

    public void increment(
            long impressions,
            long clicks,
            long basketAdditions,
            long productViews) {

        this.impressions += impressions;
        this.clicks += clicks;
        this.basketAdditions += basketAdditions;
        this.productViews += productViews;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRetailerId() {
        return retailerId;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public long getImpressions() {
        return impressions;
    }

    public long getClicks() {
        return clicks;
    }

    public long getBasketAdditions() {
        return basketAdditions;
    }

    public long getProductViews() {
        return productViews;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
