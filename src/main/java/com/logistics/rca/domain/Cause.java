package com.logistics.rca.domain;

public enum Cause {
    STOCKOUT("Stockout / inventory gap"),
    WAREHOUSE_DELAY("Warehouse processing delay"),
    SLOW_PACKING("Slow packing at warehouse"),
    SYSTEM_ISSUE("Warehouse system issue"),
    TRAFFIC("Traffic congestion"),
    WEATHER("Weather disruption"),
    ADDRESS("Incorrect or hard-to-find address"),
    VEHICLE_BREAKDOWN("Vehicle breakdown"),
    FESTIVAL("Festival / peak-event congestion"),
    HOLIDAY("Holiday-related disruption"),
    STRIKE("Strike / labor disruption"),
    CUSTOMER_RETURN("Return / unsuccessful handover"),
    COMMUNICATION("Poor delivery communication"),
    SLA_BREACH("Missed promised delivery window");

    private final String label;

    Cause(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
