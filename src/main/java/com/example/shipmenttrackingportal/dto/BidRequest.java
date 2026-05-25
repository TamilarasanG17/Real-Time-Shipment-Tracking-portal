package com.example.shipmenttrackingportal.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BidRequest {

    @NotNull(message = "Proposed price is required")
    @DecimalMin(value = "1.00", message = "Bid price must be at least 1.00")
    private BigDecimal proposedPrice;

    private String note;
}
