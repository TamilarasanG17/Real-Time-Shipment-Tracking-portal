// package com.example.shipmenttrackingportal.model;

// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import java.math.BigDecimal;
// import java.time.LocalDateTime;

// @Entity
// @Table(name = "bids")
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class Bid {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false, precision = 10, scale = 2)
//     private BigDecimal proposedPrice;

//     @Column(length = 500)
//     private String note;

//     @Column(nullable = false)
//     private boolean accepted;

//     @Column(nullable = false, updatable = false)
//     private LocalDateTime submittedAt;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "carrier_id", nullable = false)
//     private User carrier;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "shipment_id", nullable = false)
//     private Shipment shipment;

//     @PrePersist
//     protected void onCreate() {
//         this.submittedAt = LocalDateTime.now();
//         this.accepted = false;
//     }
// }


package com.example.shipmenttrackingportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id", nullable = false)
    private User carrier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BidStatus status = BidStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
