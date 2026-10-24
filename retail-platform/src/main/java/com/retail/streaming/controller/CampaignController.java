
package com.retail.streaming.controller;

import com.retail.streaming.dto.CampaignMetricsResponse;
import com.retail.streaming.services.CampaignProcessingService;
import com.retail.streaming.services.TenantAuthorizationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignProcessingService eventProcessingService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public CampaignController(
            CampaignProcessingService eventProcessingService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.eventProcessingService = eventProcessingService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @GetMapping("/{campaignId}/metrics")
    public CampaignMetricsResponse getMetrics(
            @RequestHeader("X-Retailer-Id") String retailerId,
            @PathVariable String campaignId) {

        String authorizedRetailer =
                tenantAuthorizationService.authorizeRetailer(retailerId);

        return eventProcessingService.getMetrics(
                authorizedRetailer, campaignId);
    }
}
