package com.example.shipmenttrackingportal.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.shipmenttrackingportal.model.ShipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public class ShipmentDtos {

    @Data
    public static class CreateShipmentRequest {
        @NotBlank
        private String origin;

        @NotBlank
        private String destination;

        @Positive
        private Double weightKg;

        private String description;
    }

    @Data
    public static class ShipmentResponse {
        private Long id;
        private String origin;
        private String destination;
        private Double weightKg;
        private String description;
        private ShipmentStatus status;
        private String shipperName;
        private String awardedCarrierName;
        private BigDecimal awardedPrice;
        private Double currentLat;
        private Double currentLng;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}

