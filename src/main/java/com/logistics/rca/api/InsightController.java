package com.logistics.rca.api;

import com.logistics.rca.analysis.InsightService;
import com.logistics.rca.domain.InsightReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insights;

    public InsightController(InsightService insights) {
        this.insights = insights;
    }

    @GetMapping("/city-delays")
    public InsightReport cityDelays(
            @RequestParam(defaultValue = "New Delhi") String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return insights.cityDelays(city, date);
    }

    @GetMapping("/client-failures")
    public InsightReport clientFailures(
            @RequestParam long clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return insights.clientFailures(clientId, from, to);
    }

    @GetMapping("/warehouse-failures")
    public InsightReport warehouseFailures(
            @RequestParam long warehouseId,
            @RequestParam String yearMonth
    ) {
        return insights.warehouseFailures(warehouseId, YearMonth.parse(yearMonth));
    }

    @GetMapping("/city-compare")
    public InsightReport cityCompare(
            @RequestParam String cityA,
            @RequestParam String cityB,
            @RequestParam String yearMonth
    ) {
        return insights.compareCities(cityA, cityB, YearMonth.parse(yearMonth));
    }

    @GetMapping("/festival")
    public InsightReport festival(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return insights.festivalPeriod(from, to);
    }

    @GetMapping("/capacity-risk")
    public InsightReport capacityRisk(
            @RequestParam long similarClientId,
            @RequestParam(defaultValue = "20000") int extraMonthlyOrders
    ) {
        return insights.onboardRisk(similarClientId, extraMonthlyOrders);
    }

    @GetMapping("/demo")
    public Map<String, InsightReport> demo() {
        Map<String, InsightReport> all = new LinkedHashMap<>();
        all.put("1_city_yesterday", insights.cityDelays("New Delhi", LocalDate.of(2025, 1, 24)));
        all.put("2_client_week", insights.clientFailures(409, LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 16)));
        all.put("3_warehouse_b_august", insights.warehouseFailures(2, YearMonth.of(2025, 8)));
        all.put("4_city_compare", insights.compareCities("New Delhi", "Ahmedabad", YearMonth.of(2025, 8)));
        all.put("5_festival", insights.festivalPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 9, 12)));
        all.put("6_onboard_client_y", insights.onboardRisk(118, 20_000));
        return all;
    }
}
