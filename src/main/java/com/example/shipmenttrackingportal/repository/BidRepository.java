package com.example.shipmenttrackingportal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shipmenttrackingportal.model.Bid;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByShipmentId(Long shipmentId);
    List<Bid> findByCarrierId(Long carrierId);
}
