
package com.retail.streaming.repository;

import com.retail.streaming.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByRetailerIdAndEventId(
            String retailerId, String eventId);

    @Query(value = """
        SELECT COUNT(DISTINCT anonymous_customer_id)
        FROM processed_events
        WHERE retailer_id = :retailerId
          AND campaign_id = :campaignId
          AND event_type = 'AD_CLICK'
          AND anonymous_customer_id IS NOT NULL
          AND anonymous_customer_id <> ''
        """, nativeQuery = true)
    long countUniqueClickers(
            @Param("retailerId") String retailerId,
            @Param("campaignId") String campaignId);

    @Query(value = """
        SELECT COUNT(DISTINCT anonymous_customer_id)
        FROM processed_events
        WHERE retailer_id = :retailerId
          AND campaign_id = :campaignId
          AND event_type = 'AD_IMPRESSION'
          AND anonymous_customer_id IS NOT NULL
          AND anonymous_customer_id <> ''
        """, nativeQuery = true)
    long countUniqueImpressionCustomers(
            @Param("retailerId") String retailerId,
            @Param("campaignId") String campaignId);
}
