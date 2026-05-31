package com.example.shipmenttrackingportal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shipmenttrackingportal.dto.ShipmentDtos.CreateShipmentRequest;
import com.example.shipmenttrackingportal.dto.ShipmentDtos.ShipmentResponse;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.ShipmentStatus;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;
import com.example.shipmenttrackingportal.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    // ── Shipper: Create a new load posting ───────────────────────────────────

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String shipperEmail) {
        User shipper = findUserByEmail(shipperEmail);

        Shipment shipment = Shipment.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .weightKg(request.getWeightKg())
                .description(request.getDescription())
                .shipper(shipper)
                .build();

        return toResponse(shipmentRepository.save(shipment));
    }

    // ── Public: Browse available loads (OPEN status) ─────────────────────────

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getOpenShipments() {
        return shipmentRepository.findByStatus(ShipmentStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Shipper: View own load postings ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipperLoads(String shipperEmail) {
        User shipper = findUserByEmail(shipperEmail);
        return shipmentRepository.findByShipper(shipper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Carrier: View assigned loads ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getCarrierLoads(String carrierEmail) {
        User carrier = findUserByEmail(carrierEmail);
        return shipmentRepository.findByAwardedCarrier(carrier)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Carrier: Update shipment status (IN_TRANSIT, DELIVERED) ──────────────

    @Transactional
    public ShipmentResponse updateStatus(Long shipmentId, ShipmentStatus newStatus,
                                         String carrierEmail) {
        Shipment shipment = findById(shipmentId);
        User carrier = findUserByEmail(carrierEmail);

        if (!shipment.getAwardedCarrier().getId().equals(carrier.getId())) {
            throw new AccessDeniedException(
        "You are not the assigned carrier for this shipment");
        }

        validateStatusTransition(shipment.getStatus(), newStatus);
        shipment.setStatus(newStatus);

        return toResponse(shipmentRepository.save(shipment));
    }

    // ── Get single shipment ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long id) {
        return toResponse(findById(id));
    }

    // ── Cancel shipment (shipper only) ────────────────────────────────────────

    @Transactional
    public ShipmentResponse cancelShipment(Long shipmentId, String shipperEmail) {
        Shipment shipment = findById(shipmentId);
        User shipper = findUserByEmail(shipperEmail);

        if (!shipment.getShipper().getId().equals(shipper.getId())) {
            throw new RuntimeException("Only the shipper can cancel this load");
        }
        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT
                || shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel a shipment that is in transit or delivered");
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        return toResponse(shipmentRepository.save(shipment));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Shipment findById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void validateStatusTransition(ShipmentStatus current, ShipmentStatus next) {
        boolean valid = switch (current) {
            case AWAITING_PICKUP -> next == ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT      -> next == ShipmentStatus.DELIVERED;
            default -> false;
        };
        if (!valid) {
            throw new InvalidStateTransitionException(
                    "Invalid status transition: " + current + " → " + next);
        }
    }

    public ShipmentResponse toResponse(Shipment s) {
        ShipmentResponse resp = new ShipmentResponse();
        resp.setId(s.getId());
        resp.setOrigin(s.getOrigin());
        resp.setDestination(s.getDestination());
        resp.setWeightKg(s.getWeightKg());
        resp.setDescription(s.getDescription());
        resp.setStatus(s.getStatus());
        resp.setShipperName(s.getShipper() != null ? s.getShipper().getFullName() : null);
        resp.setAwardedCarrierName(s.getAwardedCarrier() != null
                ? s.getAwardedCarrier().getFullName() : null);
        resp.setAwardedPrice(s.getAwardedPrice());
        resp.setCurrentLat(s.getCurrentLat());
        resp.setCurrentLng(s.getCurrentLng());
        resp.setCreatedAt(s.getCreatedAt());
        resp.setUpdatedAt(s.getUpdatedAt());
        return resp;
    }

    // Inner exception classes (can also be in separate file)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) { super(msg); }
    }
    public static class InvalidStateTransitionException extends RuntimeException {
        public InvalidStateTransitionException(String msg) { super(msg); }
    }
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String msg) { super(msg); }
    }
}
