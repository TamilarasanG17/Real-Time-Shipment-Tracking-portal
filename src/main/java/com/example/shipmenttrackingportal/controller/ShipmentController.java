package com.example.shipmenttrackingportal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shipmenttrackingportal.dto.ShipmentDtos.CreateShipmentRequest;
import com.example.shipmenttrackingportal.dto.ShipmentDtos.ShipmentResponse;
import com.example.shipmenttrackingportal.model.ShipmentStatus;
import com.example.shipmenttrackingportal.service.ShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Load board and shipment management")
@SecurityRequirement(name = "bearerAuth")
public class ShipmentController {

    private final ShipmentService shipmentService;

    // ── Shipper: Post a new load ───────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('SHIPPER')")
    @Operation(summary = "Post a new freight load (SHIPPER only)")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody CreateShipmentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentService.createShipment(request, user.getUsername()));
    }

    // ── Public / Carrier: Browse open loads ───────────────────────────────────

    @GetMapping
    @Operation(summary = "Browse all open shipments on the load board")
    public ResponseEntity<List<ShipmentResponse>> getOpenShipments() {
        return ResponseEntity.ok(shipmentService.getOpenShipments());
    }

    // ── Shipper: View own posted loads ────────────────────────────────────────

    @GetMapping("/my-loads")
    @PreAuthorize("hasRole('SHIPPER')")
    @Operation(summary = "View own posted loads (SHIPPER only)")
    public ResponseEntity<List<ShipmentResponse>> getMyLoads(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(shipmentService.getShipperLoads(user.getUsername()));
    }

    // ── Carrier: View assigned loads ──────────────────────────────────────────

    @GetMapping("/my-assignments")
    @PreAuthorize("hasRole('CARRIER')")
    @Operation(summary = "View carrier's assigned loads (CARRIER only)")
    public ResponseEntity<List<ShipmentResponse>> getMyAssignments(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(shipmentService.getCarrierLoads(user.getUsername()));
    }

    // ── Get single shipment ───────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get shipment details by ID")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    // ── Carrier: Update delivery status ───────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('CARRIER')")
    @Operation(summary = "Update shipment status (CARRIER only)")
    public ResponseEntity<ShipmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ShipmentStatus status,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(shipmentService.updateStatus(id, status, user.getUsername()));
    }

    // ── Shipper: Cancel a load ────────────────────────────────────────────────

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SHIPPER')")
    @Operation(summary = "Cancel a shipment (SHIPPER only)")
    public ResponseEntity<ShipmentResponse> cancelShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(shipmentService.cancelShipment(id, user.getUsername()));
    }
}
