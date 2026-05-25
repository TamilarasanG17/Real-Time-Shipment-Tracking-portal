package com.example.shipmenttrackingportal.service;


import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shipmenttrackingportal.dto.ShipmentRequest;
import com.example.shipmenttrackingportal.dto.ShipmentResponse;
import com.example.shipmenttrackingportal.enums.Role;
import com.example.shipmenttrackingportal.enums.ShipmentStatus;
import com.example.shipmenttrackingportal.exception.ResourceNotFoundException;
import com.example.shipmenttrackingportal.exception.UnauthorizedActionException;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;
import com.example.shipmenttrackingportal.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;

    
    @Transactional
    public ShipmentResponse postShipment(ShipmentRequest request) {
        User shipper = getAuthenticatedUser();

        if (shipper.getRole() != Role.SHIPPER) {
            throw new UnauthorizedActionException(
                    "Only users with role SHIPPER can post freight loads.");
        }

        Shipment shipment = Shipment.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .weightKg(request.getWeightKg())
                .description(request.getDescription())
                .status(ShipmentStatus.OPEN)
                .shipper(shipper)
                .build();

        Shipment saved = shipmentRepository.save(shipment);
        return toResponse(saved);
    }
 
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getOpenShipments() {
        return shipmentRepository.findByStatus(ShipmentStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    
    @Transactional(readOnly = true)
    public List<ShipmentResponse> getMyShipments() {
        User shipper = getAuthenticatedUser();

        if (shipper.getRole() != Role.SHIPPER) {
            throw new UnauthorizedActionException(
                    "Only users with role SHIPPER can view their posted loads.");
        }

        return shipmentRepository.findByShipperId(shipper.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);
        return toResponse(shipment);
    }

    public User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found: " + email));
    }

    public Shipment findShipmentById(long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Shipment not found with id: " + id));
    }

    public ShipmentResponse toResponse(Shipment s) {
        return ShipmentResponse.builder()
                .id(s.getId())
                .origin(s.getOrigin())
                .destination(s.getDestination())
                .weightKg(s.getWeightKg())
                .description(s.getDescription())
                .status(s.getStatus())
                .postedAt(s.getPostedAt())
                .shipperEmail(s.getShipper() != null ? s.getShipper().getEmail() : null)
                .awardedCarrierEmail(s.getCarrier() != null ? s.getCarrier().getEmail() : null)
                .build();
    }
}
