package com.logistics.rca.domain;

import java.util.EnumSet;
import java.util.List;

public record EnrichedShipment(
        OrderRecord order,
        Client client,
        Warehouse warehouse,
        List<WarehouseLog> warehouseLogs,
        FleetLog fleetLog,
        Driver driver,
        ExternalFactor externalFactor,
        List<Feedback> feedback,
        EnumSet<Cause> causes,
        Outcome outcome
) {
    public boolean isProblem() {
        return outcome == Outcome.DELAYED
                || outcome == Outcome.FAILED
                || outcome == Outcome.RETURNED
                || outcome == Outcome.OPEN_LATE;
    }
}
