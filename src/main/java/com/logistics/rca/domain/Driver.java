package com.logistics.rca.domain;

public record Driver(
        long driverId,
        String driverName,
        String partnerCompany,
        String city,
        String status
) {
}
