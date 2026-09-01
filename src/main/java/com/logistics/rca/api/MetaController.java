package com.logistics.rca.api;

import com.logistics.rca.csv.DataStore;
import com.logistics.rca.domain.Client;
import com.logistics.rca.domain.Warehouse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    private final DataStore store;

    public MetaController(DataStore store) {
        this.store = store;
    }

    @GetMapping
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("asOfDate", store.asOfDate());
        m.put("orders", store.orders().size());
        m.put("clients", store.clients().size());
        m.put("warehouses", store.warehouses().size());
        m.put("cities", store.shipments().stream()
                .map(s -> s.order().city())
                .distinct()
                .sorted()
                .toList());
        List<Map<String, Object>> clients = store.clients().values().stream()
                .sorted(Comparator.comparingLong(Client::clientId))
                .limit(20)
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("clientId", c.clientId());
                    row.put("clientName", c.clientName());
                    row.put("city", c.city());
                    return row;
                })
                .toList();
        m.put("sampleClients", clients);
        List<Map<String, Object>> warehouses = store.warehouses().values().stream()
                .sorted(Comparator.comparingLong(Warehouse::warehouseId))
                .map(w -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("warehouseId", w.warehouseId());
                    row.put("warehouseName", w.warehouseName());
                    row.put("city", w.city());
                    row.put("alias", w.warehouseId() == 2 ? "Warehouse B" : null);
                    return row;
                })
                .toList();
        m.put("warehousesDetail", warehouses);
        m.put("demoHints", Map.of(
                "cityX", "New Delhi on 2025-01-24 (highest problem city-day in the file)",
                "clientX", "409 Bath, Bhatt and Gulati",
                "warehouseB", "warehouse_id=2 (Warehouse 2, Pune)",
                "cityCompare", "New Delhi vs Ahmedabad, 2025-08",
                "clientY", "118 Atwal-Dhawan as volume proxy for a 20k-order onboard"
        ));
        return m;
    }
}
