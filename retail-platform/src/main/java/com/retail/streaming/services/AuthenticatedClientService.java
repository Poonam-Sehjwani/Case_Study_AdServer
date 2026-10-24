package com.retail.streaming.services;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedClientService {

    public String getClientId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException(
                    "Authenticated JWT is required");
        }

        // Auth0 machine-to-machine tokens commonly expose "azp".
        // Some token formats expose "client_id" instead.
        String clientId =
                jwtAuth.getToken().getClaimAsString("client_id");

        if (clientId == null || clientId.isBlank()) {
            clientId = jwtAuth.getToken().getClaimAsString("azp");
        }

        if (clientId == null || clientId.isBlank()) {
            throw new AccessDeniedException(
                    "Token does not contain a recognized client identity");
        }

        return clientId;
    }
}