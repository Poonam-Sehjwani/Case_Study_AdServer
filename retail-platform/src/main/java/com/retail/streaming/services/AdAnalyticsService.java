
package com.retail.streaming.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.retail.streaming.repository.ClickSessionRepository;
import com.retail.streaming.repository.ProcessedEventRepository;

// the read side - counts unique clickers, viewers and click-to-basket per campaign
// everything filtered by retailer so tenants stay separate
@Service
public class AdAnalyticsService {

    private final ProcessedEventRepository processedEventRepository;
    private final ClickSessionRepository clickSessionRepository;

    public AdAnalyticsService(
            ProcessedEventRepository processedEventRepository,
            ClickSessionRepository clickSessionRepository) {
        this.processedEventRepository = processedEventRepository;
        this.clickSessionRepository = clickSessionRepository;
    }

    @Transactional(readOnly = true)
    public long getUniqueClickers(String retailerId, String campaignId) {
        return processedEventRepository.countUniqueClickers(
                retailerId, campaignId);
    }

    @Transactional(readOnly = true)
    public long getUniqueImpressionCustomers(
            String retailerId, String campaignId) {
        return processedEventRepository.countUniqueImpressionCustomers(
                retailerId, campaignId);
    }

    @Transactional(readOnly = true)
    public long getUniqueClickToBasketCustomers(
            String retailerId, String campaignId) {
        return clickSessionRepository.countUniqueCustomersWithAttributedBaskets(
                retailerId, campaignId);
    }
}
