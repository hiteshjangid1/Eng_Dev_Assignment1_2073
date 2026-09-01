package com.logistics.rca.csv;

import com.logistics.rca.analysis.CauseEngine;
import com.logistics.rca.domain.Client;
import com.logistics.rca.domain.Driver;
import com.logistics.rca.domain.EnrichedShipment;
import com.logistics.rca.domain.ExternalFactor;
import com.logistics.rca.domain.Feedback;
import com.logistics.rca.domain.FleetLog;
import com.logistics.rca.domain.OrderRecord;
import com.logistics.rca.domain.Outcome;
import com.logistics.rca.domain.Warehouse;
import com.logistics.rca.domain.WarehouseLog;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Component
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final CsvSupport csv;
    private final DataStore store;
    private final CauseEngine causeEngine;

    @Value("${rca.as-of-date}")
    private LocalDate configuredAsOf;

    public DataLoader(CsvSupport csv, DataStore store, CauseEngine causeEngine) {
        this.csv = csv;
        this.store = store;
        this.causeEngine = causeEngine;
    }

    @PostConstruct
    public void load() {
        csv.map("data/clients.csv", r -> new Client(
                CsvSupport.lng(r, "client_id"),
                CsvSupport.str(r, "client_name"),
                CsvSupport.str(r, "city"),
                CsvSupport.str(r, "state"),
                CsvSupport.ts(r, "created_at")
        )).forEach(c -> store.clients().put(c.clientId(), c));

        csv.map("data/drivers.csv", r -> new Driver(
                CsvSupport.lng(r, "driver_id"),
                CsvSupport.str(r, "driver_name"),
                CsvSupport.str(r, "partner_company"),
                CsvSupport.str(r, "city"),
                CsvSupport.str(r, "status")
        )).forEach(d -> store.drivers().put(d.driverId(), d));

        csv.map("data/warehouses.csv", r -> new Warehouse(
                CsvSupport.lng(r, "warehouse_id"),
                CsvSupport.str(r, "warehouse_name"),
                CsvSupport.str(r, "city"),
                CsvSupport.str(r, "state"),
                CsvSupport.integer(r, "capacity"),
                CsvSupport.str(r, "manager_name")
        )).forEach(w -> store.warehouses().put(w.warehouseId(), w));

        store.orders().addAll(csv.map("data/orders.csv", r -> new OrderRecord(
                CsvSupport.lng(r, "order_id"),
                CsvSupport.lng(r, "client_id"),
                CsvSupport.str(r, "customer_name"),
                CsvSupport.str(r, "city"),
                CsvSupport.str(r, "state"),
                CsvSupport.ts(r, "order_date"),
                CsvSupport.ts(r, "promised_delivery_date"),
                CsvSupport.ts(r, "actual_delivery_date"),
                CsvSupport.str(r, "status"),
                CsvSupport.str(r, "payment_mode"),
                CsvSupport.dbl(r, "amount") == null ? 0.0 : CsvSupport.dbl(r, "amount"),
                CsvSupport.str(r, "failure_reason")
        )));

        csv.map("data/warehouse_logs.csv", r -> new WarehouseLog(
                CsvSupport.lng(r, "log_id"),
                CsvSupport.lng(r, "order_id"),
                CsvSupport.lng(r, "warehouse_id"),
                CsvSupport.ts(r, "picking_start"),
                CsvSupport.ts(r, "picking_end"),
                CsvSupport.ts(r, "dispatch_time"),
                CsvSupport.str(r, "notes")
        )).forEach(w -> store.warehouseLogsByOrder()
                .computeIfAbsent(w.orderId(), k -> new ArrayList<>())
                .add(w));

        csv.map("data/fleet_logs.csv", r -> new FleetLog(
                CsvSupport.lng(r, "fleet_log_id"),
                CsvSupport.lng(r, "order_id"),
                CsvSupport.lng(r, "driver_id"),
                CsvSupport.str(r, "vehicle_number"),
                CsvSupport.str(r, "route_code"),
                CsvSupport.str(r, "gps_delay_notes"),
                CsvSupport.ts(r, "departure_time"),
                CsvSupport.ts(r, "arrival_time")
        )).forEach(f -> store.fleetByOrder().putIfAbsent(f.orderId(), f));

        csv.map("data/external_factors.csv", r -> new ExternalFactor(
                CsvSupport.lng(r, "factor_id"),
                CsvSupport.lng(r, "order_id"),
                CsvSupport.str(r, "traffic_condition"),
                CsvSupport.str(r, "weather_condition"),
                CsvSupport.str(r, "event_type"),
                CsvSupport.ts(r, "recorded_at")
        )).forEach(e -> store.externalByOrder().putIfAbsent(e.orderId(), e));

        csv.map("data/feedback.csv", r -> new Feedback(
                CsvSupport.lng(r, "feedback_id"),
                CsvSupport.lng(r, "order_id"),
                CsvSupport.str(r, "customer_name"),
                CsvSupport.str(r, "feedback_text"),
                CsvSupport.str(r, "sentiment"),
                CsvSupport.str(r, "rating") == null ? null : CsvSupport.integer(r, "rating"),
                CsvSupport.ts(r, "created_at")
        )).forEach(f -> store.feedbackByOrder()
                .computeIfAbsent(f.orderId(), k -> new ArrayList<>())
                .add(f));

        store.setAsOfDate(configuredAsOf);

        for (OrderRecord order : store.orders()) {
            List<WarehouseLog> wlogs = store.warehouseLogsByOrder()
                    .getOrDefault(order.orderId(), List.of());
            Warehouse warehouse = null;
            if (!wlogs.isEmpty()) {
                warehouse = store.warehouses().get(wlogs.get(0).warehouseId());
            }
            FleetLog fleet = store.fleetByOrder().get(order.orderId());
            Driver driver = fleet == null ? null : store.drivers().get(fleet.driverId());
            ExternalFactor ext = store.externalByOrder().get(order.orderId());
            List<Feedback> fb = store.feedbackByOrder().getOrDefault(order.orderId(), List.of());
            Client client = store.clients().get(order.clientId());

            EnrichedShipment partial = new EnrichedShipment(
                    order, client, warehouse, wlogs, fleet, driver, ext, fb,
                    EnumSet.noneOf(com.logistics.rca.domain.Cause.class),
                    Outcome.IN_PROGRESS
            );
            Outcome outcome = causeEngine.outcome(partial, store.asOfDate());
            var causes = causeEngine.classify(partial, outcome);
            store.shipments().add(new EnrichedShipment(
                    order, client, warehouse, wlogs, fleet, driver, ext, fb, causes, outcome
            ));
        }

        log.info("Loaded {} orders, {} clients, {} warehouses, {} enriched shipments (as-of {})",
                store.orders().size(),
                store.clients().size(),
                store.warehouses().size(),
                store.shipments().size(),
                store.asOfDate());
    }
}
