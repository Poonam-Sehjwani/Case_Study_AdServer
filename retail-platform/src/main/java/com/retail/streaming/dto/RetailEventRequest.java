package com.retail.streaming.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RetailEventRequest(

        @NotBlank
        String eventId,

        @NotNull
        EventType eventType,

        @NotNull
        Instant eventTime,

        @NotBlank
        String retailerId,

        String sessionId,

        String productId,

        String campaignId,

        String adId,
        
        String anonymousCustomerId

) {
}