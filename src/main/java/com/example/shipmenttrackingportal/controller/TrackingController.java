package com.example.shipmenttrackingportal.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shipmenttrackingportal.dto.GpsLocationResponse;
import com.example.shipmenttrackingportal.dto.GpsPayload;
import com.example.shipmenttrackingportal.dto.LocationBroadcast;
import com.example.shipmenttrackingportal.service.TrackingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/{shipmentId}/location")
    public ResponseEntity<LocationBroadcast> receiveLocation(
            @PathVariable Long shipmentId,
            @Valid @RequestBody GpsPayload payload) {
        LocationBroadcast broadcast = trackingService.receiveAndBroadcast(shipmentId, payload);
        return ResponseEntity.ok(broadcast);
    }

    @PostMapping("/{shipmentId}/pickup")
    public ResponseEntity<Map<String, String>> confirmPickup(@PathVariable Long shipmentId) {
        trackingService.confirmPickup(shipmentId);
        return ResponseEntity.ok(Map.of(
                "message", "Pickup confirmed. Shipment is now IN_TRANSIT.",
                "shipmentId", shipmentId.toString()
        ));
    }

    @PostMapping("/{shipmentId}/delivery")
    public ResponseEntity<Map<String, String>> confirmDelivery(@PathVariable Long shipmentId) {
        trackingService.confirmDelivery(shipmentId);
        return ResponseEntity.ok(Map.of(
                "message", "Delivery confirmed. Shipment is now DELIVERED.",
                "shipmentId", shipmentId.toString()
        ));
    }

    @GetMapping("/{shipmentId}/history")
    public ResponseEntity<List<GpsLocationResponse>> getPingHistory(
            @PathVariable Long shipmentId) {
        return ResponseEntity.ok(trackingService.getPingHistory(shipmentId));
    }

    @GetMapping("/{shipmentId}/latest")
    public ResponseEntity<GpsLocationResponse> getLatestPing(
            @PathVariable Long shipmentId) {
        return ResponseEntity.ok(trackingService.getLatestPing(shipmentId));
    }
}
