// package com.example.shipmenttrackingportal.model;


// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.List;

// import com.example.shipmenttrackingportal.enums.ShipmentStatus;

// @Entity
// @Table(name = "shipments")
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class Shipment {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false, length = 255)
//     private String origin;

//     @Column(nullable = false, length = 255)
//     private String destination;

//     @Column(nullable = false, precision = 10, scale = 2)
//     private BigDecimal weightKg;

//     @Column(length = 500)
//     private String description;

//     @Enumerated(EnumType.STRING)
//     @Column(nullable = false)
//     private ShipmentStatus status;

//     @Column(nullable = false, updatable = false)
//     private LocalDateTime postedAt;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "shipper_id", nullable = false)
//     private User shipper;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "awarded_carrier_id")
//     private User carrier;

//     @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//     private List<Bid> bids;

//     @PrePersist
//     protected void onCreate() {
//         this.postedAt = LocalDateTime.now();
//         if (this.status == null) {
//             this.status = ShipmentStatus.OPEN;
//         }
//     }
// }


package com.example.shipmenttrackingportal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String origin;

    @NotBlank
    @Column(nullable = false)
    private String destination;

    @Positive
    @Column(nullable = false)
    private Double weightKg;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.OPEN;

    // Shipper who posted the load
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    // Carrier awarded the job (null until bid accepted)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "awarded_carrier_id")
    private User awardedCarrier;

    // Winning bid amount
    private BigDecimal awardedPrice;

    // Latest GPS coordinates (updated via WebSocket)
    private Double currentLat;
    private Double currentLng;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bid> bids = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}