package com.example.shipmenttrackingportal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shipmenttrackingportal.dto.ShipmentRequest;
import com.example.shipmenttrackingportal.dto.ShipmentResponse;
import com.example.shipmenttrackingportal.service.ShipmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentResponse> postShipment(
            @Valid @RequestBody ShipmentRequest request) {
        ShipmentResponse response = shipmentService.postShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/open")
    public ResponseEntity<List<ShipmentResponse>> getOpenShipments() {
        return ResponseEntity.ok(shipmentService.getOpenShipments());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ShipmentResponse>> getMyShipments() {
        return ResponseEntity.ok(shipmentService.getMyShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }
}
