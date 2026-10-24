package com.retail.streaming.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.retail.streaming.repository.ClientRetailerAccessRepository;

// checks the logged-in client is actually allowed to touch this retailer
// blocks if not. doesn't log tokens or customer data
@Service
public class TenantAuthorizationService {

    private static final Logger log =
            LoggerFactory.getLogger(TenantAuthorizationService.class);

    private final AuthenticatedClientService authenticatedClientService;
    private final ClientRetailerAccessRepository accessRepository;

    public TenantAuthorizationService(
            AuthenticatedClientService authenticatedClientService,
            ClientRetailerAccessRepository accessRepository) {
        this.authenticatedClientService = authenticatedClientService;
        this.accessRepository = accessRepository;
    }

    public String authorizeRetailer(String requestedRetailerId) {
        String clientId = authenticatedClientService.getClientId();

        if (!StringUtils.hasText(requestedRetailerId)) {
            log.warn("Tenant authorization denied: missing retailer header, clientId={}",
                    clientId);
            throw new AccessDeniedException("Retailer ID is required");
        }

        boolean allowed = accessRepository.isAuthorized(
                clientId, requestedRetailerId);

        if (!allowed) {
            // Do not log tokens, secrets, customer data, or event payloads.
            log.warn(
                "Tenant authorization denied: clientId={}, retailerId={}",
                clientId, requestedRetailerId);

            throw new AccessDeniedException(
                    "Client is not authorized for the requested retailer");
        }

        log.info(
            "Tenant authorization granted: clientId={}, retailerId={}",
            clientId, requestedRetailerId);

        return requestedRetailerId;
    }
}