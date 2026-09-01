package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record ExternalFactor(
        long factorId,
        long orderId,
        String trafficCondition,
        String weatherCondition,
        String eventType,
        LocalDateTime recordedAt
) {
}
