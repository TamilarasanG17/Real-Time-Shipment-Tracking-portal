package com.example.shipmenttrackingportal.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class LocationBroadcast {

    private Long shipmentId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private String status;
}
