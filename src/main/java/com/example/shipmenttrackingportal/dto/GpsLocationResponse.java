package com.example.shipmenttrackingportal.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class GpsLocationResponse {

    private Long id;
    private Long shipmentId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime receivedAt;
}
