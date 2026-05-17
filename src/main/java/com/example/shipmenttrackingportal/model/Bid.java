package com.example.shipmenttrackingportal.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private boolean accepted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private User carrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
        this.accepted = false;
    }
}
