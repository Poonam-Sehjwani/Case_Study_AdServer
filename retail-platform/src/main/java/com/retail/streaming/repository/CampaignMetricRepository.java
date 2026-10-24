package com.retail.streaming.repository;

import com.retail.streaming.entity.CampaignMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignMetricRepository
        extends JpaRepository<CampaignMetric, Long> {

    Optional<CampaignMetric> findByRetailerIdAndCampaignId(
            String retailerId, String campaignId);
}