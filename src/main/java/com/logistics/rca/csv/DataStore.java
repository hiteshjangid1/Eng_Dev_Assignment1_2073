package com.logistics.rca.csv;

import com.logistics.rca.domain.Client;
import com.logistics.rca.domain.Driver;
import com.logistics.rca.domain.EnrichedShipment;
import com.logistics.rca.domain.ExternalFactor;
import com.logistics.rca.domain.Feedback;
import com.logistics.rca.domain.FleetLog;
import com.logistics.rca.domain.OrderRecord;
import com.logistics.rca.domain.Warehouse;
import com.logistics.rca.domain.WarehouseLog;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataStore {

    private final Map<Long, Client> clients = new HashMap<>();
    private final Map<Long, Driver> drivers = new HashMap<>();
    private final Map<Long, Warehouse> warehouses = new HashMap<>();
    private final List<OrderRecord> orders = new ArrayList<>();
    private final Map<Long, List<WarehouseLog>> warehouseLogsByOrder = new HashMap<>();
    private final Map<Long, FleetLog> fleetByOrder = new HashMap<>();
    private final Map<Long, ExternalFactor> externalByOrder = new HashMap<>();
    private final Map<Long, List<Feedback>> feedbackByOrder = new HashMap<>();
    private final List<EnrichedShipment> shipments = new ArrayList<>();
    private LocalDate asOfDate;

    public Map<Long, Client> clients() {
        return clients;
    }

    public Map<Long, Driver> drivers() {
        return drivers;
    }

    public Map<Long, Warehouse> warehouses() {
        return warehouses;
    }

    public List<OrderRecord> orders() {
        return orders;
    }

    public Map<Long, List<WarehouseLog>> warehouseLogsByOrder() {
        return warehouseLogsByOrder;
    }

    public Map<Long, FleetLog> fleetByOrder() {
        return fleetByOrder;
    }

    public Map<Long, ExternalFactor> externalByOrder() {
        return externalByOrder;
    }

    public Map<Long, List<Feedback>> feedbackByOrder() {
        return feedbackByOrder;
    }

    public List<EnrichedShipment> shipments() {
        return shipments;
    }

    public LocalDate asOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }
}
