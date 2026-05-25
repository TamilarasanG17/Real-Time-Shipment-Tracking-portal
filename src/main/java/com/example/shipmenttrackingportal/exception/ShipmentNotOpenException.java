package com.example.shipmenttrackingportal.exception;

public class ShipmentNotOpenException extends RuntimeException {
    public ShipmentNotOpenException(String message) {
        super(message);
    }
}
