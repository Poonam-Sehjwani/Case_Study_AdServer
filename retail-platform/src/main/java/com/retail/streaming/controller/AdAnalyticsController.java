
package com.retail.streaming.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retail.streaming.services.AdAnalyticsService;
import com.retail.streaming.services.TenantAuthorizationService;

@RestController
@RequestMapping("/ad")
public class AdAnalyticsController {

    private final AdAnalyticsService adAnalyticsService;
    private final TenantAuthorizationService tenantAuthorizationService;

    public AdAnalyticsController(
            AdAnalyticsService adAnalyticsService,
            TenantAuthorizationService tenantAuthorizationService) {
        this.adAnalyticsService = adAnalyticsService;
        this.tenantAuthorizationService = tenantAuthorizationService;
    }

    @GetMapping("/{campaignId}/clicks")
    public long getClicks(
            @PathVariable String campaignId,
            @RequestHeader("X-Retailer-Id") String retailerId) {

        String authorizedRetailer =
                tenantAuthorizationService.authorizeRetailer(retailerId);

        return adAnalyticsService.getUniqueClickers(
                authorizedRetailer, campaignId);
    }

    @GetMapping("/{campaignId}/impressions")
    public long getImpressions(
            @PathVariable String campaignId,
            @RequestHeader("X-Retailer-Id") String retailerId) {

        String authorizedRetailer =
                tenantAuthorizationService.authorizeRetailer(retailerId);

        return adAnalyticsService.getUniqueImpressionCustomers(
                authorizedRetailer, campaignId);
    }

    @GetMapping("/{campaignId}/clickToBasket")
    public long getClickToBasket(
            @PathVariable String campaignId,
            @RequestHeader("X-Retailer-Id") String retailerId) {

        String authorizedRetailer =
                tenantAuthorizationService.authorizeRetailer(retailerId);

        return adAnalyticsService.getUniqueClickToBasketCustomers(
                authorizedRetailer, campaignId);
    }
}
