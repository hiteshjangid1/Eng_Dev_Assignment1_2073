package com.logistics.rca.domain;

public record Warehouse(
        long warehouseId,
        String warehouseName,
        String city,
        String state,
        int capacity,
        String managerName
) {
    public String displayName() {
        return warehouseName + " (" + city + ")";
    }
}
