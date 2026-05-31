package com.example.shipmenttrackingportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TrackingDtos {

   
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsUpdatePayload {
        private Double latitude;
        private Double longitude;
        private Long shipmentId;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingBroadcast {
        private Long shipmentId;
        private Double latitude;
        private Double longitude;
        private String status;
        private long timestamp;
    }
}

