package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record OrderRecord(
        long orderId,
        long clientId,
        String customerName,
        String city,
        String state,
        LocalDateTime orderDate,
        LocalDateTime promisedDeliveryDate,
        LocalDateTime actualDeliveryDate,
        String status,
        String paymentMode,
        double amount,
        String failureReason
) {
}
