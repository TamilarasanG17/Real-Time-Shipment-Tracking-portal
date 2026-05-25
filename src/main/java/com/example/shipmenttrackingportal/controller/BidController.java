package com.example.shipmenttrackingportal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.shipmenttrackingportal.dto.BidRequest;
import com.example.shipmenttrackingportal.dto.BidResponse;
import com.example.shipmenttrackingportal.service.BidService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    @PostMapping("/api/shipments/{shipmentId}/bids")
    public ResponseEntity<BidResponse> submitBid(
            @PathVariable Long shipmentId,
            @Valid @RequestBody BidRequest request) {
        BidResponse response = bidService.submitBid(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/shipments/{shipmentId}/bids")
    public ResponseEntity<List<BidResponse>> getBidsForShipment(
            @PathVariable Long shipmentId) {
        return ResponseEntity.ok(bidService.getBidsForShipment(shipmentId));
    }

    @PostMapping("/api/shipments/{shipmentId}/bids/{bidId}/award")
    public ResponseEntity<BidResponse> awardBid(
            @PathVariable Long shipmentId,
            @PathVariable Long bidId) {
        BidResponse response = bidService.awardBid(shipmentId, bidId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/bids/mine")
    public ResponseEntity<List<BidResponse>> getMyBids() {
        return ResponseEntity.ok(bidService.getMyBids());
    }
}
