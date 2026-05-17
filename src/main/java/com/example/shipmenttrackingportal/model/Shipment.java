package com.example.shipmenttrackingportal.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.shipmenttrackingportal.enums.ShipmentStatus;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String origin;

    @Column(nullable = false, length = 255)
    private String destination;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_carrier_id")
    private User carrier;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids;

    @PrePersist
    protected void onCreate() {
        this.postedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ShipmentStatus.OPEN;
        }
    }
}
