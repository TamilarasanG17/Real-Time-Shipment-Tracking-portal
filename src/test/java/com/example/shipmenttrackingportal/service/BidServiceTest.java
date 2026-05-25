package com.example.shipmenttrackingportal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.shipmenttrackingportal.dto.BidRequest;
import com.example.shipmenttrackingportal.dto.BidResponse;
import com.example.shipmenttrackingportal.enums.Role;
import com.example.shipmenttrackingportal.enums.ShipmentStatus;
import com.example.shipmenttrackingportal.exception.ResourceNotFoundException;
import com.example.shipmenttrackingportal.exception.ShipmentNotOpenException;
import com.example.shipmenttrackingportal.exception.UnauthorizedActionException;
import com.example.shipmenttrackingportal.model.Bid;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.BidRepository;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private BidService bidService;

    private User shipper;
    private User carrier;
    private User anotherCarrier;
    private Shipment openShipment;
    private Bid carrierBid;
    private Bid anotherCarrierBid;

    @BeforeEach
    void setUp() {
        shipper = User.builder()
                .id(1L).email("shipper@test.com").role(Role.SHIPPER).build();

        carrier = User.builder()
                .id(2L).email("carrier@test.com").role(Role.CARRIER).build();

        anotherCarrier = User.builder()
                .id(3L).email("other.carrier@test.com").role(Role.CARRIER).build();

        openShipment = Shipment.builder()
                .id(10L)
                .origin("Mumbai")
                .destination("Bengaluru")
                .weightKg(new BigDecimal("500"))
                .status(ShipmentStatus.OPEN)
                .shipper(shipper)
                .postedAt(LocalDateTime.now())
                .build();

        carrierBid = Bid.builder()
                .id(100L)
                .proposedPrice(new BigDecimal("15000"))
                .accepted(false)
                .carrier(carrier)
                .shipment(openShipment)
                .submittedAt(LocalDateTime.now())
                .build();

        anotherCarrierBid = Bid.builder()
                .id(101L)
                .proposedPrice(new BigDecimal("18000"))
                .accepted(false)
                .carrier(anotherCarrier)
                .shipment(openShipment)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    // -----------------------------------------------------------------------
    // submitBid() tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("submitBid: CARRIER successfully bids on an OPEN shipment")
    void submitBid_Success() {
        BidRequest request = new BidRequest();
        request.setProposedPrice(new BigDecimal("15000"));
        request.setNote("Fast delivery");

        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);
        when(bidRepository.save(any(Bid.class))).thenReturn(carrierBid);

        BidResponse response = bidService.submitBid(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getProposedPrice()).isEqualByComparingTo("15000");
        assertThat(response.isAccepted()).isFalse();
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    @DisplayName("submitBid: SHIPPER cannot submit a bid — throws UnauthorizedActionException")
    void submitBid_ShipperRole_ThrowsUnauthorized() {
        BidRequest request = new BidRequest();
        request.setProposedPrice(new BigDecimal("10000"));

        when(shipmentService.getAuthenticatedUser()).thenReturn(shipper); // SHIPPER trying to bid

        assertThatThrownBy(() -> bidService.submitBid(10L, request))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("CARRIER");

        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitBid: Cannot bid on a non-OPEN shipment — throws ShipmentNotOpenException")
    void submitBid_ShipmentNotOpen_ThrowsConflict() {
        openShipment.setStatus(ShipmentStatus.AWAITING_PICKUP); // Already awarded
        BidRequest request = new BidRequest();
        request.setProposedPrice(new BigDecimal("12000"));

        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);

        assertThatThrownBy(() -> bidService.submitBid(10L, request))
                .isInstanceOf(ShipmentNotOpenException.class)
                .hasMessageContaining("AWAITING_PICKUP");

        verify(bidRepository, never()).save(any());
    }

    @Test
    @DisplayName("awardBid: SHIPPER awards a bid — shipment locked, losing bids rejected")
    void awardBid_Success_AtomicTransaction() {
        when(shipmentService.getAuthenticatedUser()).thenReturn(shipper);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);
        when(bidRepository.findById(100L)).thenReturn(Optional.of(carrierBid));
        when(bidRepository.findByShipmentId(10L))
                .thenReturn(List.of(carrierBid, anotherCarrierBid));
        when(bidRepository.save(any(Bid.class))).thenReturn(carrierBid);

        BidResponse response = bidService.awardBid(10L, 100L);

        // Winning bid is accepted
        assertThat(carrierBid.isAccepted()).isTrue();

        // Losing bid is explicitly rejected
        assertThat(anotherCarrierBid.isAccepted()).isFalse();

        // Shipment status transitioned to AWAITING_PICKUP
        assertThat(openShipment.getStatus()).isEqualTo(ShipmentStatus.AWAITING_PICKUP);

        // Winning carrier is linked to the shipment
        assertThat(openShipment.getCarrier()).isEqualTo(carrier);

        // Verify the shipment was saved after state transition
        verify(shipmentRepository, times(1)).save(openShipment);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("awardBid: CARRIER cannot award a bid — throws UnauthorizedActionException")
    void awardBid_CarrierRole_ThrowsUnauthorized() {
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);

        assertThatThrownBy(() -> bidService.awardBid(10L, 100L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("SHIPPER");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("awardBid: Shipper cannot award a bid on another shipper's load")
    void awardBid_WrongShipper_ThrowsUnauthorized() {
        User anotherShipper = User.builder()
                .id(99L).email("other.shipper@test.com").role(Role.SHIPPER).build();

        when(shipmentService.getAuthenticatedUser()).thenReturn(anotherShipper);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);

        assertThatThrownBy(() -> bidService.awardBid(10L, 100L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("own shipments");
    }

    @Test
    @DisplayName("awardBid: Cannot award on an already-awarded shipment — throws ShipmentNotOpenException")
    void awardBid_AlreadyAwarded_ThrowsConflict() {
        openShipment.setStatus(ShipmentStatus.AWAITING_PICKUP);

        when(shipmentService.getAuthenticatedUser()).thenReturn(shipper);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);

        assertThatThrownBy(() -> bidService.awardBid(10L, 100L))
                .isInstanceOf(ShipmentNotOpenException.class)
                .hasMessageContaining("AWAITING_PICKUP");
    }

    @Test
    @DisplayName("awardBid: Bid not found — throws ResourceNotFoundException")
    void awardBid_BidNotFound_ThrowsNotFound() {
        when(shipmentService.getAuthenticatedUser()).thenReturn(shipper);
        when(shipmentService.findShipmentById(10L)).thenReturn(openShipment);
        when(bidRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bidService.awardBid(10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getMyBids: CARRIER retrieves their own submitted bids")
    void getMyBids_Carrier_Success() {
        when(shipmentService.getAuthenticatedUser()).thenReturn(carrier);
        when(bidRepository.findByCarrierId(2L)).thenReturn(List.of(carrierBid));

        List<BidResponse> bids = bidService.getMyBids();

        assertThat(bids).hasSize(1);
        assertThat(bids.get(0).getCarrierEmail()).isEqualTo("carrier@test.com");
    }

    @Test
    @DisplayName("getMyBids: SHIPPER cannot call this endpoint — throws UnauthorizedActionException")
    void getMyBids_ShipperRole_ThrowsUnauthorized() {
        when(shipmentService.getAuthenticatedUser()).thenReturn(shipper);

        assertThatThrownBy(() -> bidService.getMyBids())
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("CARRIER");
    }
}
