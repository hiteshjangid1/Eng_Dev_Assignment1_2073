package com.logistics.rca.domain;

import java.time.LocalDateTime;

public record FleetLog(
        long fleetLogId,
        long orderId,
        long driverId,
        String vehicleNumber,
        String routeCode,
        String gpsDelayNotes,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {
}
