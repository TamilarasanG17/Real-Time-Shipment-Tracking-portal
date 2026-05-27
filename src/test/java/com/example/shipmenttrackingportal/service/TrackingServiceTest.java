package com.example.shipmenttrackingportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.shipmenttrackingportal.dto.GpsLocationResponse;
import com.example.shipmenttrackingportal.dto.GpsPayload;
import com.example.shipmenttrackingportal.dto.LocationBroadcast;
import com.example.shipmenttrackingportal.enums.Role;
import com.example.shipmenttrackingportal.enums.ShipmentStatus;
import com.example.shipmenttrackingportal.exception.ResourceNotFoundException;
import com.example.shipmenttrackingportal.exception.UnauthorizedActionException;
import com.example.shipmenttrackingportal.model.GpsLocation;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.GpsLocationRepository;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock private GpsLocationRepository gpsLocationRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ShipmentService shipmentService;

    @InjectMocks
    private TrackingService trackingService;

    private User shipper;
    private User carrier;
    private Shipment inTransitShipment;
    private Shipment awaitingPickupShipment;

    @BeforeEach
    void setUp() {
        shipper = User.builder()
                .id(1L).email("shipper@test.com").role(Role.SHIPPER).build();

        carrier = User.builder()
                .id(2L).email("carrier@test.com").role(Role.CARRIER).build();

        inTransitShipment = Shipment.builder()
                .id(10L)
                .origin("Mumbai")
                .destination("Bengaluru")
                .status(ShipmentStatus.IN_TRANSIT)
                .shipper(shipper)
                .carrier(carrier)
                .postedAt(LocalDateTime.now())
                .build();

        awaitingPickupShipment = Shipment.builder()
                .id(11L)
                .origin("Chennai")
                .destination("Delhi")
                .status(ShipmentStatus.AWAITING_PICKUP)
                .shipper(shipper)
                .carrier(carrier)
                .postedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("receiveAndBroadcast: broadcasts to correct WebSocket topic and persists ping")
    void receiveAndBroadcast_Success_BroadcastsAndPersists() {
        GpsPayload payload = new GpsPayload();
        payload.setLatitude(18.9220);
        payload.setLongitude(72.8347);

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(gpsLocationRepository.save(any(GpsLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocationBroadcast result = trackingService.receiveAndBroadcast(10L, payload);

        // Verify broadcast content
        assertThat(result.getShipmentId()).isEqualTo(10L);
        assertThat(result.getLatitude()).isEqualTo(18.9220);
        assertThat(result.getLongitude()).isEqualTo(72.8347);
        assertThat(result.getStatus()).isEqualTo("IN_TRANSIT");

        // Verify broadcast destination is correct WebSocket topic
        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocationBroadcast> payloadCaptor =
                ArgumentCaptor.forClass(LocationBroadcast.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), payloadCaptor.capture());

        assertThat(destCaptor.getValue()).isEqualTo("/topic/shipment/10");
        assertThat(payloadCaptor.getValue().getLatitude()).isEqualTo(18.9220);

        // Verify GPS ping was persisted
        verify(gpsLocationRepository, times(1)).save(any(GpsLocation.class));
    }

    @Test
    @DisplayName("receiveAndBroadcast: rejects ping if shipment is not IN_TRANSIT")
    void receiveAndBroadcast_NotInTransit_ThrowsUnauthorized() {
        GpsPayload payload = new GpsPayload();
        payload.setLatitude(12.9716);
        payload.setLongitude(77.5946);

        // Shipment is AWAITING_PICKUP — not yet in transit
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(awaitingPickupShipment));

        assertThatThrownBy(() -> trackingService.receiveAndBroadcast(11L, payload))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("IN_TRANSIT");

        // Verify no broadcast or DB write happened
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        verify(gpsLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("receiveAndBroadcast: throws ResourceNotFoundException for unknown shipment")
    void receiveAndBroadcast_ShipmentNotFound_ThrowsNotFound() {
        GpsPayload payload = new GpsPayload();
        payload.setLatitude(10.0);
        payload.setLongitude(77.0);

        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.receiveAndBroadcast(999L, payload))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("confirmPickup: transitions AWAITING_PICKUP → IN_TRANSIT and broadcasts status")
    void confirmPickup_Success_TransitionsStatus() {
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(awaitingPickupShipment));
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(awaitingPickupShipment);

        trackingService.confirmPickup(11L);

        // Status transitioned
        assertThat(awaitingPickupShipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(shipmentRepository, times(1)).save(awaitingPickupShipment);

        // Status-change broadcast was sent
        verify(messagingTemplate).convertAndSend(
                eq("/topic/shipment/11"), any(LocationBroadcast.class));
    }

    @Test
    @DisplayName("confirmPickup: non-carrier cannot confirm pickup")
    void confirmPickup_WrongCarrier_ThrowsUnauthorized() {
        User wrongCarrier = User.builder()
                .id(99L).email("impostor@test.com").role(Role.CARRIER).build();

        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(awaitingPickupShipment));
        when(shipmentService.getAuthenticatedUser()).thenReturn(wrongCarrier);

        assertThatThrownBy(() -> trackingService.confirmPickup(11L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("awarded carrier");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmPickup: cannot confirm pickup if already IN_TRANSIT")
    void confirmPickup_AlreadyInTransit_ThrowsUnauthorized() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);

        assertThatThrownBy(() -> trackingService.confirmPickup(10L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("AWAITING_PICKUP");
    }

    @Test
    @DisplayName("confirmDelivery: transitions IN_TRANSIT → DELIVERED and broadcasts final status")
    void confirmDelivery_Success_TransitionsStatus() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(inTransitShipment);

        trackingService.confirmDelivery(10L);

        assertThat(inTransitShipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        verify(shipmentRepository, times(1)).save(inTransitShipment);

        // Final broadcast with DELIVERED status
        ArgumentCaptor<LocationBroadcast> broadcastCaptor =
                ArgumentCaptor.forClass(LocationBroadcast.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/shipment/10"), broadcastCaptor.capture());

        assertThat(broadcastCaptor.getValue().getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("confirmDelivery: cannot confirm delivery from AWAITING_PICKUP state")
    void confirmDelivery_NotInTransit_ThrowsUnauthorized() {
        when(shipmentRepository.findById(11L)).thenReturn(Optional.of(awaitingPickupShipment));
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);

        assertThatThrownBy(() -> trackingService.confirmDelivery(11L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("IN_TRANSIT");
    }
    
    @Test
    @DisplayName("getPingHistory: returns all pings for a shipment ordered by receivedAt DESC")
    void getPingHistory_ReturnsPingList() {
        GpsLocation ping1 = GpsLocation.builder()
                .id(1L).latitude(18.92).longitude(72.83)
                .shipment(inTransitShipment).receivedAt(LocalDateTime.now().minusMinutes(5)).build();
        GpsLocation ping2 = GpsLocation.builder()
                .id(2L).latitude(18.95).longitude(72.85)
                .shipment(inTransitShipment).receivedAt(LocalDateTime.now()).build();

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(gpsLocationRepository.findByShipmentIdOrderByReceivedAtDesc(10L))
                .thenReturn(List.of(ping2, ping1)); // most recent first

        List<GpsLocationResponse> history = trackingService.getPingHistory(10L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getLatitude()).isEqualTo(18.95); // most recent first
        assertThat(history.get(1).getLatitude()).isEqualTo(18.92);
    }

    @Test
    @DisplayName("getLatestPing: returns the most recent GPS ping")
    void getLatestPing_ReturnsLatest() {
        GpsLocation latest = GpsLocation.builder()
                .id(5L).latitude(12.97).longitude(77.59)
                .shipment(inTransitShipment).receivedAt(LocalDateTime.now()).build();

        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(gpsLocationRepository.findTopByShipmentIdOrderByReceivedAtDesc(10L))
                .thenReturn(Optional.of(latest));

        GpsLocationResponse response = trackingService.getLatestPing(10L);

        assertThat(response.getLatitude()).isEqualTo(12.97);
        assertThat(response.getLongitude()).isEqualTo(77.59);
    }

    @Test
    @DisplayName("getLatestPing: throws ResourceNotFoundException when no pings exist")
    void getLatestPing_NoPings_ThrowsNotFound() {
        when(shipmentRepository.findById(10L)).thenReturn(Optional.of(inTransitShipment));
        when(gpsLocationRepository.findTopByShipmentIdOrderByReceivedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackingService.getLatestPing(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No GPS pings found");
    }
}
