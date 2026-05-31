package com.example.shipmenttrackingportal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shipmenttrackingportal.dto.BidDtos.BidResponse;
import com.example.shipmenttrackingportal.dto.BidDtos.PlaceBidRequest;
import com.example.shipmenttrackingportal.model.Bid;
import com.example.shipmenttrackingportal.model.BidStatus;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.ShipmentStatus;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.BidRepository;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;
import com.example.shipmenttrackingportal.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final BidRepository bidRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    // ── Carrier: Place a bid on an open shipment ──────────────────────────────

    @Transactional
    public BidResponse placeBid(Long shipmentId, PlaceBidRequest request, String carrierEmail) {
        Shipment shipment = findShipmentById(shipmentId);
        User carrier = findUserByEmail(carrierEmail);

        if (shipment.getStatus() != ShipmentStatus.OPEN) {
            throw new IllegalStateException("Bids can only be placed on OPEN shipments");
        }

        if (bidRepository.existsByShipmentAndCarrier(shipment, carrier)) {
            throw new IllegalStateException("You have already placed a bid on this shipment");
        }

        Bid bid = Bid.builder()
                .shipment(shipment)
                .carrier(carrier)
                .amount(request.getAmount())
                .notes(request.getNotes())
                .build();

        return toResponse(bidRepository.save(bid));
    }

    // ── Shipper: Accept a bid (atomic - reject all others) ────────────────────

    @Transactional
    public BidResponse acceptBid(Long bidId, String shipperEmail) {
        Bid winningBid = findBidById(bidId);
        Shipment shipment = winningBid.getShipment();
        User shipper = findUserByEmail(shipperEmail);

        // Authorization: only the shipment's owner can accept bids
        if (!shipment.getShipper().getId().equals(shipper.getId())) {
            throw new SecurityException("Only the shipper who posted this load can accept bids");
        }

        if (shipment.getStatus() != ShipmentStatus.OPEN) {
            throw new IllegalStateException(
                    "This shipment is no longer accepting bids (status: " + shipment.getStatus() + ")");
        }

        // ── Atomic operation: accept winner, reject all others ─────────────────
        // 1. Reject all pending bids for this shipment
        List<Bid> allBids = bidRepository.findByShipment(shipment);
        allBids.forEach(bid -> bid.setStatus(BidStatus.REJECTED));
        bidRepository.saveAll(allBids);

        // 2. Mark the winning bid as ACCEPTED
        winningBid.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(winningBid);

        // 3. Lock the shipment to the winning carrier
        shipment.setStatus(ShipmentStatus.AWAITING_PICKUP);
        shipment.setAwardedCarrier(winningBid.getCarrier());
        shipment.setAwardedPrice(winningBid.getAmount());
        shipmentRepository.save(shipment);

        log.info("Bid {} accepted for shipment {}. Carrier: {}",
                bidId, shipment.getId(), winningBid.getCarrier().getEmail());

        return toResponse(winningBid);
    }

    // ── Get all bids for a shipment (shipper view) ────────────────────────────

    @Transactional(readOnly = true)
    public List<BidResponse> getBidsForShipment(Long shipmentId, String shipperEmail) {
        Shipment shipment = findShipmentById(shipmentId);
        User shipper = findUserByEmail(shipperEmail);

        if (!shipment.getShipper().getId().equals(shipper.getId())) {
            throw new SecurityException("You can only view bids on your own shipments");
        }

        return bidRepository.findByShipment(shipment)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Get carrier's own bids ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids(String carrierEmail) {
        User carrier = findUserByEmail(carrierEmail);
        return bidRepository.findByCarrier(carrier)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Shipment findShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + id));
    }

    private Bid findBidById(Long id) {
        return bidRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bid not found: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private BidResponse toResponse(Bid b) {
        BidResponse resp = new BidResponse();
        resp.setId(b.getId());
        resp.setShipmentId(b.getShipment().getId());
        resp.setCarrierName(b.getCarrier().getFullName());
        resp.setCarrierEmail(b.getCarrier().getEmail());
        resp.setAmount(b.getAmount());
        resp.setNotes(b.getNotes());
        resp.setStatus(b.getStatus());
        resp.setCreatedAt(b.getCreatedAt());
        return resp;
    }
}