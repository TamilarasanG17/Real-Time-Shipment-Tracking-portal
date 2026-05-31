package com.example.shipmenttrackingportal.model;

public enum ShipmentStatus {
    OPEN,               // Posted by shipper, accepting bids
    AWAITING_PICKUP,    // Bid accepted, carrier assigned
    IN_TRANSIT,         // Carrier has picked up and is en route
    DELIVERED,          // Successfully delivered
    CANCELLED           // Cancelled by shipper
}
