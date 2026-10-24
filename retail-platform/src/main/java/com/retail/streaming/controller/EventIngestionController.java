package com.retail.streaming.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.retail.streaming.dto.RetailEventRequest;
import com.retail.streaming.services.EventIngestionService;
import com.retail.streaming.services.TenantAuthorizationService;

import jakarta.validation.Valid;

// takes events in, checks the client is allowed for this retailer, then sends to kafka
@RestController
@RequestMapping("/api/v1/events")
public class EventIngestionController {

	private final EventIngestionService ingestionService;
	private final TenantAuthorizationService tenantAuthorizationService;

	public EventIngestionController(EventIngestionService ingestionService,
			TenantAuthorizationService tenantAuthorizationService) {
		this.ingestionService = ingestionService;
		this.tenantAuthorizationService = tenantAuthorizationService;

	}

	@PostMapping
	public ResponseEntity<String> ingestEvent(@Valid @RequestBody RetailEventRequest event,
			@RequestHeader("X-Retailer-Id") String retailerId) {

		String authorizedRetailer = tenantAuthorizationService.authorizeRetailer(retailerId);

		// header retailer and body retailer must match, else reject
		if (!authorizedRetailer.equals(event.retailerId())) {
			throw new AccessDeniedException("Retailer mismatch");
		}

		String result = ingestionService.ingest(event);
		return ResponseEntity.ok(result);
	}

}
