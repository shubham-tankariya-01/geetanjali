package com.geetanjali.backend.enums;

// PaymentMode: Supported payment methods in the Geetanjali tailor shop.
// Tailors primarily collect advance deposits and balance settlements via Cash or UPI (Google Pay, PhonePe, Paytm).
public enum PaymentMode {
    // Physical cash received in shop register
    CASH,

    // Digital payments received via UPI QR code or mobile number
    UPI
}
