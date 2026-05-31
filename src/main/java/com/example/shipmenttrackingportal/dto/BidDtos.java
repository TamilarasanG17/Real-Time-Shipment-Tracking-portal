package com.example.shipmenttrackingportal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.shipmenttrackingportal.model.BidStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

public class BidDtos {

    @Data
    public static class PlaceBidRequest {
        @NotNull
        @Positive
        private BigDecimal amount;

        private String notes;
    }

    @Data
    public static class BidResponse {
        private Long id;
        private Long shipmentId;
        private String carrierName;
        private String carrierEmail;
        private BigDecimal amount;
        private String notes;
        private BidStatus status;
        private LocalDateTime createdAt;
    }
}
