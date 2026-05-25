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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.shipmenttrackingportal.dto.ShipmentRequest;
import com.example.shipmenttrackingportal.dto.ShipmentResponse;
import com.example.shipmenttrackingportal.enums.Role;
import com.example.shipmenttrackingportal.enums.ShipmentStatus;
import com.example.shipmenttrackingportal.exception.UnauthorizedActionException;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.User;
import com.example.shipmenttrackingportal.repository.ShipmentRepository;
import com.example.shipmenttrackingportal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @InjectMocks
    private ShipmentService shipmentService;

    private User shipper;
    private User carrier;
    private Shipment openShipment;

    @BeforeEach
    void setUp() {
        shipper = User.builder()
                .id(1L).email("shipper@test.com").role(Role.SHIPPER).build();

        carrier = User.builder()
                .id(2L).email("carrier@test.com").role(Role.CARRIER).build();

        openShipment = Shipment.builder()
                .id(10L)
                .origin("Chennai")
                .destination("Delhi")
                .weightKg(new BigDecimal("200"))
                .status(ShipmentStatus.OPEN)
                .shipper(shipper)
                .postedAt(LocalDateTime.now())
                .build();

        // Wire mock security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    @DisplayName("postShipment: SHIPPER successfully posts a freight load")
    void postShipment_Shipper_Success() {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Chennai");
        request.setDestination("Delhi");
        request.setWeightKg(new BigDecimal("200"));

        when(authentication.getName()).thenReturn("shipper@test.com");
        when(userRepository.findByEmail("shipper@test.com")).thenReturn(Optional.of(shipper));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(openShipment);

        ShipmentResponse response = shipmentService.postShipment(request);

        assertThat(response.getOrigin()).isEqualTo("Chennai");
        assertThat(response.getStatus()).isEqualTo(ShipmentStatus.OPEN);
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("postShipment: CARRIER cannot post a load — throws UnauthorizedActionException")
    void postShipment_CarrierRole_ThrowsUnauthorized() {
        ShipmentRequest request = new ShipmentRequest();
        request.setOrigin("Pune");
        request.setDestination("Hyderabad");
        request.setWeightKg(new BigDecimal("100"));

        when(authentication.getName()).thenReturn("carrier@test.com");
        when(userRepository.findByEmail("carrier@test.com")).thenReturn(Optional.of(carrier));

        assertThatThrownBy(() -> shipmentService.postShipment(request))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("SHIPPER");

        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOpenShipments: Returns all OPEN shipments from repository")
    void getOpenShipments_ReturnsList() {
        when(shipmentRepository.findByStatus(ShipmentStatus.OPEN))
                .thenReturn(List.of(openShipment));

        List<ShipmentResponse> result = shipmentService.getOpenShipments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ShipmentStatus.OPEN);
    }

    @Test
    @DisplayName("getMyShipments: CARRIER cannot call this endpoint")
    void getMyShipments_CarrierRole_ThrowsUnauthorized() {
        when(authentication.getName()).thenReturn("carrier@test.com");
        when(userRepository.findByEmail("carrier@test.com")).thenReturn(Optional.of(carrier));

        assertThatThrownBy(() -> shipmentService.getMyShipments())
                .isInstanceOf(UnauthorizedActionException.class);
    }
}
