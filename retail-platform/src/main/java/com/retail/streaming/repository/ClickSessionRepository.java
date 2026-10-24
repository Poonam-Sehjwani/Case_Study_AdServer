
package com.retail.streaming.repository;

import com.retail.streaming.entity.ClickSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickSessionRepository
        extends JpaRepository<ClickSession, Long> {

    List<ClickSession>
    findByRetailerIdAndSessionIdAndAttributedAtIsNullAndClickedAtBetweenOrderByClickedAtDesc(
            String retailerId,
            String sessionId,
            Instant from,
            Instant to);

    @Query(value = """
        SELECT COUNT(DISTINCT anonymous_customer_id)
        FROM click_sessions
        WHERE retailer_id = :retailerId
          AND campaign_id = :campaignId
          AND attributed_at IS NOT NULL
          AND anonymous_customer_id IS NOT NULL
          AND anonymous_customer_id <> ''
        """, nativeQuery = true)
    long countUniqueCustomersWithAttributedBaskets(
            @Param("retailerId") String retailerId,
            @Param("campaignId") String campaignId);
}
