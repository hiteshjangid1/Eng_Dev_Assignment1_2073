package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record Client(
        long clientId,
        String clientName,
        String city,
        String state,
        LocalDateTime createdAt
) {
}
