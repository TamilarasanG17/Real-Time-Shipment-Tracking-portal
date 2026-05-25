package com.example.shipmenttrackingportal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentService shipmentService; 

    @Transactional
    public BidResponse submitBid(long shipmentId, BidRequest request) {
        User carrier = shipmentService.getAuthenticatedUser();

        if (carrier.getRole() != Role.CARRIER) {
            throw new UnauthorizedActionException(
                    "Only users with role CARRIER can submit bids.");
        }

        Shipment shipment = shipmentService.findShipmentById(shipmentId);

        if (shipment.getStatus() != ShipmentStatus.OPEN) {
            throw new ShipmentNotOpenException(
                    "Shipment #" + shipmentId + " is not open for bidding. " +
                    "Current status: " + shipment.getStatus());
        }

        Bid bid = Bid.builder()
                .proposedPrice(request.getProposedPrice())
                .note(request.getNote())
                .accepted(false)
                .carrier(carrier)
                .shipment(shipment)
                .build();

        Bid saved = bidRepository.save(bid);
        return toResponse(saved);
    }

    @Transactional
    public BidResponse awardBid(long shipmentId, long bidId) {
        User shipper = shipmentService.getAuthenticatedUser();

        if (shipper.getRole() != Role.SHIPPER) {
            throw new UnauthorizedActionException(
                    "Only users with role SHIPPER can award bids.");
        }

        Shipment shipment = shipmentService.findShipmentById(shipmentId);

        if (!shipment.getShipper().getId().equals(shipper.getId())) {
            throw new UnauthorizedActionException(
                    "You can only award bids on your own shipments.");
        }

        if (shipment.getStatus() != ShipmentStatus.OPEN) {
            throw new ShipmentNotOpenException(
                    "Shipment #" + shipmentId + " is no longer open. " +
                    "Current status: " + shipment.getStatus());
        }

        Bid winningBid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bid not found with id: " + bidId));

        if (!winningBid.getShipment().getId().equals(shipmentId)) {
            throw new ResourceNotFoundException(
                    "Bid #" + bidId + " does not belong to Shipment #" + shipmentId);
        }

        winningBid.setAccepted(true);
        bidRepository.save(winningBid);
        List<Bid> allBids = bidRepository.findByShipmentId(shipmentId);
        List<Bid> rejectedBids = allBids.stream()
                .filter(b -> !b.getId().equals(bidId))
                .peek(b -> b.setAccepted(false))
                .collect(Collectors.toList());
        bidRepository.saveAll(rejectedBids);

        shipment.setStatus(ShipmentStatus.AWAITING_PICKUP);
        shipment.setCarrier(winningBid.getCarrier());
        shipmentRepository.save(shipment);

        return toResponse(winningBid);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidsForShipment(Long shipmentId) {
        User caller = shipmentService.getAuthenticatedUser();
        Shipment shipment = shipmentService.findShipmentById(shipmentId);

        if (caller.getRole() == Role.SHIPPER &&
                !shipment.getShipper().getId().equals(caller.getId())) {
            throw new UnauthorizedActionException(
                    "You can only view bids on your own shipments.");
        }

        return bidRepository.findByShipmentId(shipmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getMyBids() {
        User carrier = shipmentService.getAuthenticatedUser();

        if (carrier.getRole() != Role.CARRIER) {
            throw new UnauthorizedActionException(
                    "Only users with role CARRIER can view their submitted bids.");
        }

        return bidRepository.findByCarrierId(carrier.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BidResponse toResponse(Bid b) {
        return BidResponse.builder()
                .id(b.getId())
                .proposedPrice(b.getProposedPrice())
                .note(b.getNote())
                .accepted(b.isAccepted())
                .submittedAt(b.getSubmittedAt())
                .carrierEmail(b.getCarrier() != null ? b.getCarrier().getEmail() : null)
                .shipmentId(b.getShipment() != null ? b.getShipment().getId() : null)
                .build();
    }
}
