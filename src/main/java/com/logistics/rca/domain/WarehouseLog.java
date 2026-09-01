package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record WarehouseLog(
        long logId,
        long orderId,
        long warehouseId,
        LocalDateTime pickingStart,
        LocalDateTime pickingEnd,
        LocalDateTime dispatchTime,
        String notes
) {
    public long pickMinutes() {
        if (pickingStart == null || pickingEnd == null) {
            return 0;
        }
        return java.time.Duration.between(pickingStart, pickingEnd).toMinutes();
    }

    public long dockToDispatchMinutes() {
        if (pickingEnd == null || dispatchTime == null) {
            return 0;
        }
        return java.time.Duration.between(pickingEnd, dispatchTime).toMinutes();
    }
}
