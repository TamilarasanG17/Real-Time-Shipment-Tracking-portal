package com.example.shipmenttrackingportal.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shipmenttrackingportal.dto.TrackingDtos.GpsUpdatePayload;
import com.example.shipmenttrackingportal.dto.TrackingDtos.TrackingBroadcast;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public void processGpsUpdate(GpsUpdatePayload payload) {
        Shipment shipment = shipmentRepository.findById(payload.getShipmentId())
                .orElseThrow(() -> new RuntimeException(
                        "Shipment not found: " + payload.getShipmentId()));

        // Persist the latest GPS coordinates
        shipment.setCurrentLat(payload.getLatitude());
        shipment.setCurrentLng(payload.getLongitude());
        shipmentRepository.save(shipment);

        // Broadcast to all subscribers of this shipment's topic
        TrackingBroadcast broadcast = new TrackingBroadcast(
                shipment.getId(),
                payload.getLatitude(),
                payload.getLongitude(),
                shipment.getStatus().name(),
                System.currentTimeMillis()
        );

        String topic = "/topic/tracking/" + shipment.getId();
        messagingTemplate.convertAndSend(topic, broadcast);

        log.debug("GPS update broadcast for shipment {} → lat={}, lng={}",
                shipment.getId(), payload.getLatitude(), payload.getLongitude());
    }

    @Transactional(readOnly = true)
    public TrackingBroadcast getLastKnownPosition(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + shipmentId));

        return new TrackingBroadcast(
                shipment.getId(),
                shipment.getCurrentLat(),
                shipment.getCurrentLng(),
                shipment.getStatus().name(),
                System.currentTimeMillis()
        );
    }
}
