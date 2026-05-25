package com.example.shipmenttrackingportal.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidResponse {

    private Long id;
    private BigDecimal proposedPrice;
    private String note;
    private boolean accepted;
    private LocalDateTime submittedAt;
    private String carrierEmail;   // Carrier identity
    private Long shipmentId;
}
