package com.example.shipmenttrackingportal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shipmenttrackingportal.dto.BidDtos.BidResponse;
import com.example.shipmenttrackingportal.dto.BidDtos.PlaceBidRequest;
import com.example.shipmenttrackingportal.service.BidService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Carrier bidding and shipper acceptance endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BidController {

    private final BidService bidService;

    // ── Carrier: Place a bid on a shipment ────────────────────────────────────

    @PostMapping("/shipment/{shipmentId}")
    @PreAuthorize("hasRole('CARRIER')")
    @Operation(summary = "Place a bid on an open shipment (CARRIER only)")
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable Long shipmentId,
            @Valid @RequestBody PlaceBidRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bidService.placeBid(shipmentId, request, user.getUsername()));
    }

    // ── Shipper: View all bids for their shipment ─────────────────────────────

    @GetMapping("/shipment/{shipmentId}")
    @PreAuthorize("hasRole('SHIPPER')")
    @Operation(summary = "Get all bids for a specific shipment (SHIPPER only)")
    public ResponseEntity<List<BidResponse>> getBidsForShipment(
            @PathVariable Long shipmentId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bidService.getBidsForShipment(shipmentId, user.getUsername()));
    }

    // ── Shipper: Accept a bid (rejects all others atomically) ────────────────

    @PostMapping("/{bidId}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    @Operation(summary = "Accept a bid - atomically rejects all competing bids (SHIPPER only)")
    public ResponseEntity<BidResponse> acceptBid(
            @PathVariable Long bidId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bidService.acceptBid(bidId, user.getUsername()));
    }

    // ── Carrier: View own submitted bids ─────────────────────────────────────

    @GetMapping("/my-bids")
    @PreAuthorize("hasRole('CARRIER')")
    @Operation(summary = "View carrier's own submitted bids (CARRIER only)")
    public ResponseEntity<List<BidResponse>> getMyBids(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bidService.getMyBids(user.getUsername()));
    }
}
