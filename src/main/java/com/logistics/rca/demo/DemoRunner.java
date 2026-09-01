package com.logistics.rca.demo;

import com.logistics.rca.analysis.InsightService;
import com.logistics.rca.domain.InsightReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(10)
public class DemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

    private final InsightService insights;

    public DemoRunner(InsightService insights) {
        this.insights = insights;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, InsightReport> demos = new LinkedHashMap<>();
        demos.put("Use case 1 — City delays (New Delhi, 2025-01-24 as a high-signal 'yesterday')",
                insights.cityDelays("New Delhi", LocalDate.of(2025, 1, 24)));
        demos.put("Use case 2 — Client X failures (Bath, Bhatt and Gulati / id 409, week 2025-08-10 to 2025-08-16)",
                insights.clientFailures(409, LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 16)));
        demos.put("Use case 3 — Warehouse B (id 2 / Pune) in August 2025",
                insights.warehouseFailures(2, YearMonth.of(2025, 8)));
        demos.put("Use case 4 — New Delhi vs Ahmedabad, August 2025",
                insights.compareCities("New Delhi", "Ahmedabad", YearMonth.of(2025, 8)));
        demos.put("Use case 5 — Festival-tagged orders vs baseline (2025-01-01 to 2025-09-12)",
                insights.festivalPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 9, 12)));
        demos.put("Use case 6 — Onboard Client Y (~20,000 extra monthly orders; proxy client 118 Atwal-Dhawan)",
                insights.onboardRisk(118, 20_000));

        StringBuilder out = new StringBuilder();
        out.append("Delivery Root-Cause Analyzer — sample use case outputs\n");
        out.append("Generated locally from the eight CSV domains (orders, clients, warehouses,\n");
        out.append("warehouse logs, fleet logs, drivers, external factors, feedback).\n");
        out.append("Calendar note: the files cover 2025-01-01 to 2025-09-12, so 'yesterday'\n");
        out.append("and 'last month' are interpreted against that operational calendar, not the OS clock.\n\n");

        int i = 1;
        for (Map.Entry<String, InsightReport> e : demos.entrySet()) {
            out.append("-".repeat(80)).append("\n");
            out.append(e.getKey()).append("\n");
            out.append("-".repeat(80)).append("\n");
            out.append(e.getValue().toPlainText()).append("\n");
            log.info("\n\n==== Demo {} ====\n{}", i++, e.getValue().toPlainText());
        }

        Path dir = Path.of("reports");
        Files.createDirectories(dir);
        Path file = dir.resolve("sample-use-case-outputs.txt");
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        log.info("Wrote {}", file.toAbsolutePath());
    }
}
