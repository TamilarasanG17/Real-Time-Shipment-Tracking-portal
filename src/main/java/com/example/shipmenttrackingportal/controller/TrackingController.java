package com.example.shipmenttrackingportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.shipmenttrackingportal.dto.TrackingDtos.GpsUpdatePayload;
import com.example.shipmenttrackingportal.dto.TrackingDtos.TrackingBroadcast;
import com.example.shipmenttrackingportal.service.TrackingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@Tag(name = "Tracking", description = "Real-time GPS tracking via WebSocket and STOMP")
public class TrackingController {

    private final TrackingService trackingService;

    @MessageMapping("/tracking/{shipmentId}")
    public void receiveGpsUpdate(
            @DestinationVariable Long shipmentId,
            GpsUpdatePayload payload) {
        payload.setShipmentId(shipmentId);
        trackingService.processGpsUpdate(payload);
    }

    @GetMapping("/api/tracking/{shipmentId}")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get last known GPS position for a shipment")
    public ResponseEntity<TrackingBroadcast> getLastPosition(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(trackingService.getLastKnownPosition(shipmentId));
    }
}