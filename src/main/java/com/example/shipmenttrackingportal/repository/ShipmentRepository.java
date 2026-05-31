package com.example.shipmenttrackingportal.repository;

// import java.util.List;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import com.example.shipmenttrackingportal.enums.ShipmentStatus;
// import com.example.shipmenttrackingportal.model.Shipment;

// @Repository
// public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
//     List<Shipment> findByShipperId(Long shipperId);
//     List<Shipment> findByStatus(ShipmentStatus status);
// }

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.ShipmentStatus;
import com.example.shipmenttrackingportal.model.User;


@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByShipper(User shipper);

    List<Shipment> findByStatus(ShipmentStatus status);

    List<Shipment> findByAwardedCarrier(User carrier);
}

