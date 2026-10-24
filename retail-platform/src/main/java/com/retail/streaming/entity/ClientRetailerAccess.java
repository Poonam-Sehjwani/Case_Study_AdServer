package com.retail.streaming.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "client_retailer_access",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_client_retailer",
        columnNames = {"client_id", "retailer_id"}
    )
)
public class ClientRetailerAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;

    @Column(name = "retailer_id", nullable = false, length = 100)
    private String retailerId;

    @Column(nullable = false)
    private boolean enabled = true;

    protected ClientRetailerAccess() {
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRetailerId() {
        return retailerId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}