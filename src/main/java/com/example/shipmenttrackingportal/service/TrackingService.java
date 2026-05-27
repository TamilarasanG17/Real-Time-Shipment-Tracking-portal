package com.example.shipmenttrackingportal.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shipmenttrackingportal.dto.GpsLocationResponse;
import com.example.shipmenttrackingportal.dto.GpsPayload;
import com.example.shipmenttrackingportal.dto.LocationBroadcast;
import com.example.shipmenttrackingportal.enums.ShipmentStatus;
import com.example.shipmenttrackingportal.exception.ResourceNotFoundException;
import com.example.shipmenttrackingportal.exception.UnauthorizedActionException;
import com.example.shipmenttrackingportal.model.GpsLocation;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.repository.GpsLocationRepository;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final GpsLocationRepository gpsLocationRepository;
    private final ShipmentRepository shipmentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ShipmentService shipmentService;

    @Transactional
    public LocationBroadcast receiveAndBroadcast(Long shipmentId, GpsPayload payload) {
        Shipment shipment = findShipment(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new UnauthorizedActionException(
                    "GPS pings can only be sent for shipments with status IN_TRANSIT. " +
                    "Current status: " + shipment.getStatus() +
                    ". Use /api/tracking/" + shipmentId + "/pickup to start transit.");
        }

        LocalDateTime now = LocalDateTime.now();

        LocationBroadcast broadcast = LocationBroadcast.builder()
                .shipmentId(shipmentId)
                .latitude(payload.getLatitude())
                .longitude(payload.getLongitude())
                .timestamp(now)
                .status(shipment.getStatus().name())
                .build();

        String destination = "/topic/shipment/" + shipmentId;
        messagingTemplate.convertAndSend(destination, broadcast);

        log.info("GPS broadcast → {} | lat={} lng={} | shipment={}",
                destination, payload.getLatitude(), payload.getLongitude(), shipmentId);

        // --- Step 3: Persist the ping for audit / route replay ---
        GpsLocation location = GpsLocation.builder()
                .latitude(payload.getLatitude())
                .longitude(payload.getLongitude())
                .shipment(shipment)
                .build();
        gpsLocationRepository.save(location);

        return broadcast;
    }

    @Transactional
    public void confirmPickup(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        String callerEmail = shipmentService.getAuthenticatedUser().getEmail();

        if (shipment.getCarrier() == null ||
                !shipment.getCarrier().getEmail().equals(callerEmail)) {
            throw new UnauthorizedActionException(
                    "Only the awarded carrier can confirm pickup for shipment #" + shipmentId);
        }

        if (shipment.getStatus() != ShipmentStatus.AWAITING_PICKUP) {
            throw new UnauthorizedActionException(
                    "Pickup can only be confirmed for AWAITING_PICKUP shipments. " +
                    "Current status: " + shipment.getStatus());
        }

        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipmentRepository.save(shipment);

        LocationBroadcast statusUpdate = LocationBroadcast.builder()
                .shipmentId(shipmentId)
                .latitude(null)
                .longitude(null)
                .timestamp(LocalDateTime.now())
                .status(ShipmentStatus.IN_TRANSIT.name())
                .build();
        messagingTemplate.convertAndSend("/topic/shipment/" + shipmentId, statusUpdate);

        log.info("Shipment #{} → IN_TRANSIT (carrier: {})", shipmentId, callerEmail);
    }

    @Transactional
    public void confirmDelivery(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        String callerEmail = shipmentService.getAuthenticatedUser().getEmail();

        if (shipment.getCarrier() == null ||
                !shipment.getCarrier().getEmail().equals(callerEmail)) {
            throw new UnauthorizedActionException(
                    "Only the awarded carrier can confirm delivery for shipment #" + shipmentId);
        }

        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new UnauthorizedActionException(
                    "Delivery can only be confirmed for IN_TRANSIT shipments. " +
                    "Current status: " + shipment.getStatus());
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipmentRepository.save(shipment);

        LocationBroadcast deliveryUpdate = LocationBroadcast.builder()
                .shipmentId(shipmentId)
                .latitude(null)
                .longitude(null)
                .timestamp(LocalDateTime.now())
                .status(ShipmentStatus.DELIVERED.name())
                .build();
        messagingTemplate.convertAndSend("/topic/shipment/" + shipmentId, deliveryUpdate);

        log.info("Shipment #{} → DELIVERED (carrier: {})", shipmentId, callerEmail);
    }

    @Transactional(readOnly = true)
    public List<GpsLocationResponse> getPingHistory(Long shipmentId) {
        findShipment(shipmentId); // Validates shipment exists
        return gpsLocationRepository
                .findByShipmentIdOrderByReceivedAtDesc(shipmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GpsLocationResponse getLatestPing(Long shipmentId) {
        findShipment(shipmentId);
        return gpsLocationRepository
                .findTopByShipmentIdOrderByReceivedAtDesc(shipmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No GPS pings found for shipment #" + shipmentId));
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found with id: " + shipmentId));
    }

    private GpsLocationResponse toResponse(GpsLocation loc) {
        return GpsLocationResponse.builder()
                .id(loc.getId())
                .shipmentId(loc.getShipment().getId())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .receivedAt(loc.getReceivedAt())
                .build();
    }
}