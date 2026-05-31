package com.example.shipmenttrackingportal.repository;

// import java.util.List;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import com.example.shipmenttrackingportal.model.Bid;

// @Repository
// public interface BidRepository extends JpaRepository<Bid, Long> {
//     List<Bid> findByShipmentId(Long shipmentId);
//     List<Bid> findByCarrierId(Long carrierId);
// }

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shipmenttrackingportal.model.Bid;
import com.example.shipmenttrackingportal.model.BidStatus;
import com.example.shipmenttrackingportal.model.Shipment;
import com.example.shipmenttrackingportal.model.User;


@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    List<Bid> findByShipment(Shipment shipment);

    List<Bid> findByCarrier(User carrier);

    List<Bid> findByShipmentAndStatus(Shipment shipment, BidStatus status);

    boolean existsByShipmentAndCarrier(Shipment shipment, User carrier);
}
