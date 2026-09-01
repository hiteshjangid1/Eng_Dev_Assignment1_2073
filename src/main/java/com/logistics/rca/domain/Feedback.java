package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record Feedback(
        long feedbackId,
        long orderId,
        String customerName,
        String feedbackText,
        String sentiment,
        Integer rating,
        LocalDateTime createdAt
) {
}
