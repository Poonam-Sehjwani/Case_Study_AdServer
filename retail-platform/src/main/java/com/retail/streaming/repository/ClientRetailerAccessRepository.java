package com.retail.streaming.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.retail.streaming.entity.ClientRetailerAccess;

public interface ClientRetailerAccessRepository
        extends JpaRepository<ClientRetailerAccess, Long> {

    @Query("""
        SELECT COUNT(a) > 0
        FROM ClientRetailerAccess a
        WHERE a.clientId = :clientId
          AND a.retailerId = :retailerId
          AND a.enabled = true
        """)
    boolean isAuthorized(
            @Param("clientId") String clientId,
            @Param("retailerId") String retailerId);
}