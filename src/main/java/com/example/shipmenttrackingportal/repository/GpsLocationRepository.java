package com.example.shipmenttrackingportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shipmenttrackingportal.model.GpsLocation;

@Repository
public interface GpsLocationRepository extends JpaRepository<GpsLocation, Long> {
    List<GpsLocation> findByShipmentIdOrderByReceivedAtDesc(Long shipmentId);

    Optional<GpsLocation> findTopByShipmentIdOrderByReceivedAtDesc(Long shipmentId);
}
