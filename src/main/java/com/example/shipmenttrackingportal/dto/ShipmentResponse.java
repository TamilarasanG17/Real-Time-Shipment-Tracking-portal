package com.example.shipmenttrackingportal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.shipmenttrackingportal.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ShipmentResponse {

    private Long id;
    private String origin;
    private String destination;
    private BigDecimal weightKg;
    private String description;
    private ShipmentStatus status;
    private LocalDateTime postedAt;
    private String shipperEmail; 
    private String awardedCarrierEmail;
}
