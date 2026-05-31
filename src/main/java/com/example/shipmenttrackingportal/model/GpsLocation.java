// package com.example.shipmenttrackingportal.model;

// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import java.time.LocalDateTime;

// @Entity
// @Table(name = "gps_locations", indexes = {
//     @Index(name = "idx_gps_shipment_received", columnList = "shipment_id, received_at DESC")
// })
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class GpsLocation {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @Column(nullable = false)
//     private Double latitude;

//     @Column(nullable = false)
//     private Double longitude;

//     @Column(nullable = false, updatable = false)
//     private LocalDateTime receivedAt;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "shipment_id", nullable = false)
//     private Shipment shipment;

//     @PrePersist
//     protected void onCreate() {
//         this.receivedAt = LocalDateTime.now();
//     }
// }