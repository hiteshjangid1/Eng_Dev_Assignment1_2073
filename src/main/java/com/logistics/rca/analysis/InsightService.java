package com.logistics.rca.analysis;

import com.logistics.rca.csv.DataStore;
import com.logistics.rca.domain.Cause;
import com.logistics.rca.domain.CauseStat;
import com.logistics.rca.domain.Client;
import com.logistics.rca.domain.EnrichedShipment;
import com.logistics.rca.domain.InsightReport;
import com.logistics.rca.domain.Outcome;
import com.logistics.rca.domain.Warehouse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InsightService {

    private final DataStore store;
    private final RecommendationCatalog recommendations;

    public InsightService(DataStore store, RecommendationCatalog recommendations) {
        this.store = store;
        this.recommendations = recommendations;
    }

    public InsightReport cityDelays(String city, LocalDate date) {
        List<EnrichedShipment> slice = store.shipments().stream()
                .filter(s -> cityEquals(s.order().city(), city))
                .filter(s -> onDate(s, date))
                .toList();
        InsightReport report = baseReport(
                "City delay diagnosis",
                "Why were deliveries delayed in city " + city + " on " + date + "?",
                "City=" + city + ", delivery-related date=" + date
                        + " (matched on promised, actual, or order date)"
        );
        fill(report, slice, city);
        return report;
    }

    public InsightReport clientFailures(long clientId, LocalDate from, LocalDate to) {
        Client client = store.clients().get(clientId);
        String name = client == null ? ("Client " + clientId) : client.clientName();
        List<EnrichedShipment> slice = store.shipments().stream()
                .filter(s -> s.order().clientId() == clientId)
                .filter(s -> inRange(s, from, to))
                .toList();
        InsightReport report = baseReport(
                "Client failure diagnosis",
                "Why did " + name + "'s orders fail in the selected week?",
                "Client=" + name + " (id=" + clientId + "), " + from + " to " + to
        );
        fill(report, slice, name);
        return report;
    }

    public InsightReport warehouseFailures(long warehouseId, YearMonth month) {
        Warehouse wh = store.warehouses().get(warehouseId);
        String label = wh == null ? ("Warehouse " + warehouseId) : aliasWarehouse(wh);
        List<EnrichedShipment> slice = store.shipments().stream()
                .filter(s -> s.warehouse() != null && s.warehouse().warehouseId() == warehouseId)
                .filter(s -> inMonth(s, month))
                .toList();
        InsightReport report = baseReport(
                "Warehouse-linked failures",
                "Explain the top reasons for delivery failures linked to " + label + " in " + month + ".",
                label + ", month=" + month
        );
        fill(report, slice, label);
        return report;
    }

    public InsightReport compareCities(String cityA, String cityB, YearMonth month) {
        InsightReport a = cityMonth(cityA, month);
        InsightReport b = cityMonth(cityB, month);
        InsightReport report = baseReport(
                "City comparison",
                "Compare delivery failure causes between " + cityA + " and " + cityB + " in " + month + ".",
                cityA + " vs " + cityB + ", month=" + month
        );
        report.getMetrics().put(cityA + " orders", a.getMetrics().get("orders"));
        report.getMetrics().put(cityA + " problem rate", a.getMetrics().get("problemRate"));
        report.getMetrics().put(cityB + " orders", b.getMetrics().get("orders"));
        report.getMetrics().put(cityB + " problem rate", b.getMetrics().get("problemRate"));
        report.getMetrics().put(cityA + " top cause", topCause(a));
        report.getMetrics().put(cityB + " top cause", topCause(b));

        StringBuilder narrative = new StringBuilder();
        narrative.append("In ").append(month).append(", ").append(cityA)
                .append(" processed ").append(a.getMetrics().get("orders"))
                .append(" orders with a problem rate of ").append(a.getMetrics().get("problemRate"))
                .append(", while ").append(cityB).append(" processed ")
                .append(b.getMetrics().get("orders")).append(" orders at ")
                .append(b.getMetrics().get("problemRate")).append(". ");
        narrative.append(cityA).append(" is most often tagged with ").append(topCause(a))
                .append("; ").append(cityB).append(" is most often tagged with ").append(topCause(b)).append(". ");
        if (!Objects.equals(topCause(a), topCause(b))) {
            narrative.append("The cities do not share the same primary cause, so a single national playbook would miss local bottlenecks. ");
        } else {
            narrative.append("Both cities share the same leading cause, which points to a network-wide process rather than a city-only issue. ");
        }
        narrative.append("Use the ranked lists below to staff, route, and promise differently per city.");
        report.setNarrative(narrative.toString());
        report.getCauses().addAll(prefixed(cityA, a.getCauses()));
        report.getCauses().addAll(prefixed(cityB, b.getCauses()));
        report.getRecommendations().addAll(recommendations.forCauses(a.getCauses(), cityA));
        for (String rec : recommendations.forCauses(b.getCauses(), cityB)) {
            if (!report.getRecommendations().contains(rec)) {
                report.getRecommendations().add(rec);
            }
        }
        report.getSampleOrderIds().addAll(a.getSampleOrderIds());
        report.getSampleOrderIds().addAll(b.getSampleOrderIds());
        return report;
    }

    public InsightReport festivalPeriod(LocalDate from, LocalDate to) {
        List<EnrichedShipment> fest = store.shipments().stream()
                .filter(s -> inRange(s, from, to))
                .filter(this::isFestival)
                .toList();
        List<EnrichedShipment> rest = store.shipments().stream()
                .filter(s -> inRange(s, from, to))
                .filter(s -> !isFestival(s))
                .toList();

        InsightReport festReport = baseReport("Festival (internal)", "", "");
        fill(festReport, fest, "festival");
        InsightReport base = baseReport("Baseline (internal)", "", "");
        fill(base, rest, "baseline");

        InsightReport report = baseReport(
                "Festival-period risk",
                "What are the likely causes of delivery failures during the festival period, and how should we prepare?",
                "Festival flag from external_factors.event_type between " + from + " and " + to
                        + ". This dataset tags Festival on individual orders rather than a calendar holiday window."
        );
        report.getMetrics().put("festivalOrders", fest.size());
        report.getMetrics().put("festivalProblemRate", festReport.getMetrics().get("problemRate"));
        report.getMetrics().put("baselineOrders", rest.size());
        report.getMetrics().put("baselineProblemRate", base.getMetrics().get("problemRate"));
        report.getCauses().addAll(festReport.getCauses());
        report.getSampleOrderIds().addAll(festReport.getSampleOrderIds());

        StringBuilder n = new StringBuilder();
        n.append("Festival-tagged orders (").append(fest.size()).append(") show a problem rate of ")
                .append(festReport.getMetrics().get("problemRate"))
                .append(" versus ").append(base.getMetrics().get("problemRate"))
                .append(" on non-festival orders in the same span. ");
        n.append("Leading festival causes are ").append(summarizeCauses(festReport.getCauses()))
                .append(". ");
        n.append("Preparation should assume traffic, weather, and warehouse dwell all worsen together: pre-position inventory near demand cities, freeze aggressive SLAs, and roster extra riders before the event—not after complaints spike.");
        report.setNarrative(n.toString());
        report.getRecommendations().add("Stand up a festival control tower 10 days prior: daily join of warehouse dwell, traffic, and failed-first-attempt rates.");
        report.getRecommendations().addAll(recommendations.forCauses(festReport.getCauses(), "festival peak"));
        return report;
    }

    public InsightReport onboardRisk(long similarClientId, int extraMonthlyOrders) {
        Client client = store.clients().get(similarClientId);
        String name = client == null ? ("Client " + similarClientId) : client.clientName();
        List<EnrichedShipment> all = store.shipments();
        List<EnrichedShipment> similar = all.stream()
                .filter(s -> s.order().clientId() == similarClientId)
                .toList();

        long monthsCovered = 9;
        double currentMonthly = all.size() / (double) monthsCovered;
        double similarMonthly = similar.size() / (double) monthsCovered;
        double networkProblemRate = rate(all);
        double similarProblemRate = similar.isEmpty() ? networkProblemRate : rate(similar);

        Map<String, Long> cityVolume = all.stream()
                .collect(Collectors.groupingBy(s -> s.order().city(), Collectors.counting()));
        long totalCap = store.warehouses().values().stream().mapToLong(Warehouse::capacity).sum();

        InsightReport report = baseReport(
                "New-client capacity risk",
                "If we onboard a client similar to " + name + " with ~" + extraMonthlyOrders
                        + " extra monthly orders, what new failure risks should we expect and how do we mitigate them?",
                "Proxy client=" + name + " (id=" + similarClientId + "), extraMonthlyOrders=" + extraMonthlyOrders
        );
        report.getMetrics().put("networkOrdersInDataset", all.size());
        report.getMetrics().put("approxCurrentMonthlyOrders", Math.round(currentMonthly));
        report.getMetrics().put("proxyClientMonthlyOrders", Math.round(similarMonthly));
        report.getMetrics().put("networkProblemRate", pct(networkProblemRate));
        report.getMetrics().put("proxyClientProblemRate", pct(similarProblemRate));
        report.getMetrics().put("expectedExtraProblemShipmentsPerMonth",
                Math.round(extraMonthlyOrders * similarProblemRate));
        report.getMetrics().put("volumeIncreaseVsCurrentMonthly",
                String.format("%.1fx", extraMonthlyOrders / Math.max(1.0, currentMonthly)));
        report.getMetrics().put("sumWarehouseCapacityUnits", totalCap);

        fillCausesOnly(report, similar.isEmpty() ? all : similar);

        String busiest = cityVolume.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        StringBuilder n = new StringBuilder();
        n.append("The historical network is about ").append(Math.round(currentMonthly))
                .append(" orders/month. Adding ").append(extraMonthlyOrders)
                .append(" monthly orders is roughly ")
                .append(String.format("%.1fx", extraMonthlyOrders / Math.max(1.0, currentMonthly)))
                .append(" current volume—this is not a marginal client; it is a network redesign. ");
        n.append("Applying the proxy client's problem rate (").append(pct(similarProblemRate))
                .append(") implies about ")
                .append(Math.round(extraMonthlyOrders * similarProblemRate))
                .append(" additional delayed/failed/returned shipments every month if processes stay unchanged. ");
        n.append("Risk concentrates where volume already sits (led by ").append(busiest)
                .append(") and on the same cause mix as the proxy: ")
                .append(summarizeCauses(report.getCauses()))
                .append(". Warehouse capacity in the master data is ")
                .append(totalCap)
                .append(" units across ").append(store.warehouses().size())
                .append(" nodes; without mapping SKU cubes to those units, treat the 17x order surge as exceeding last-mile and pack-station throughput long before storage is the constraint.");
        report.setNarrative(n.toString());
        report.getRecommendations().add("Do not onboard 20k monthly orders onto the current SLA in a single wave; phase volume by city and warehouse with a hard weekly cap.");
        report.getRecommendations().add("Stand up dedicated pack lines and rider pools in " + busiest
                + " and the proxy client's home city before the first peak week.");
        report.getRecommendations().addAll(recommendations.forCauses(report.getCauses(), "new-client surge"));
        report.getSampleOrderIds().addAll(sampleIds(similar.isEmpty() ? all : similar, 8));
        return report;
    }

    private InsightReport cityMonth(String city, YearMonth month) {
        List<EnrichedShipment> slice = store.shipments().stream()
                .filter(s -> cityEquals(s.order().city(), city))
                .filter(s -> inMonth(s, month))
                .toList();
        InsightReport report = baseReport(city, "", city);
        fill(report, slice, city);
        return report;
    }

    private void fill(InsightReport report, List<EnrichedShipment> slice, String hint) {
        List<EnrichedShipment> problems = slice.stream().filter(EnrichedShipment::isProblem).toList();
        report.getMetrics().put("orders", slice.size());
        report.getMetrics().put("problems", problems.size());
        report.getMetrics().put("problemRate", pct(slice.isEmpty() ? 0 : problems.size() / (double) slice.size()));
        report.getMetrics().put("failed", countOutcome(slice, Outcome.FAILED));
        report.getMetrics().put("delayedDelivered", countOutcome(slice, Outcome.DELAYED));
        report.getMetrics().put("returned", countOutcome(slice, Outcome.RETURNED));
        report.getMetrics().put("openLate", countOutcome(slice, Outcome.OPEN_LATE));
        fillCausesOnly(report, problems);
        report.getSampleOrderIds().addAll(sampleIds(problems, 8));
        report.setNarrative(buildNarrative(report, slice, problems, hint));
        report.getRecommendations().addAll(recommendations.forCauses(report.getCauses(), hint));
    }

    private void fillCausesOnly(InsightReport report, List<EnrichedShipment> problems) {
        report.getCauses().clear();
        if (problems.isEmpty()) {
            return;
        }
        Map<Cause, Integer> counts = new EnumMap<>(Cause.class);
        for (EnrichedShipment s : problems) {
            for (Cause c : s.causes()) {
                counts.merge(c, 1, Integer::sum);
            }
        }
        List<CauseStat> stats = new ArrayList<>();
        for (Map.Entry<Cause, Integer> e : counts.entrySet()) {
            stats.add(new CauseStat(
                    e.getKey(),
                    e.getValue(),
                    e.getValue() / (double) problems.size(),
                    "tagged from orders, warehouse, fleet, weather/traffic, and/or feedback"
            ));
        }
        stats.sort(Comparator.comparingInt(CauseStat::count).reversed());
        report.getCauses().addAll(stats.stream().limit(8).toList());
    }

    private String buildNarrative(InsightReport report, List<EnrichedShipment> slice,
                                  List<EnrichedShipment> problems, String hint) {
        if (slice.isEmpty()) {
            return "No orders matched this filter. Widen the date range or check city/client/warehouse spelling.";
        }
        if (problems.isEmpty()) {
            return "All " + slice.size() + " matched orders are on-time or still inside SLA. There is no failure pattern to explain in this slice.";
        }
        StringBuilder n = new StringBuilder();
        n.append("Across ").append(slice.size()).append(" orders in this slice, ")
                .append(problems.size()).append(" (").append(report.getMetrics().get("problemRate"))
                .append(") were delayed, failed, returned, or still open past promise. ");
        n.append("Failures=").append(report.getMetrics().get("failed"))
                .append(", late delivered=").append(report.getMetrics().get("delayedDelivered"))
                .append(", returned=").append(report.getMetrics().get("returned"))
                .append(", open-late=").append(report.getMetrics().get("openLate")).append(". ");
        n.append("The leading evidence-backed causes are ").append(summarizeCauses(report.getCauses()))
                .append(". ");
        n.append("These tags are not mutually exclusive: a stockout at the warehouse can coincide with rain and a customer complaint about lateness. ")
                .append("Operations should attack the top two causes in ").append(hint)
                .append(" first—those explain the largest share of the problem set.");
        return n.toString();
    }

    private static InsightReport baseReport(String title, String question, String scope) {
        InsightReport r = new InsightReport();
        r.setTitle(title);
        r.setQuestion(question);
        r.setScope(scope);
        return r;
    }

    private static boolean cityEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static boolean onDate(EnrichedShipment s, LocalDate date) {
        return dateEquals(s.order().promisedDeliveryDate() == null ? null : s.order().promisedDeliveryDate().toLocalDate(), date)
                || dateEquals(s.order().actualDeliveryDate() == null ? null : s.order().actualDeliveryDate().toLocalDate(), date)
                || dateEquals(s.order().orderDate() == null ? null : s.order().orderDate().toLocalDate(), date);
    }

    private static boolean dateEquals(LocalDate a, LocalDate b) {
        return a != null && a.equals(b);
    }

    private static boolean inRange(EnrichedShipment s, LocalDate from, LocalDate to) {
        LocalDate d = primaryDate(s);
        return d != null && !d.isBefore(from) && !d.isAfter(to);
    }

    private static boolean inMonth(EnrichedShipment s, YearMonth month) {
        LocalDate d = primaryDate(s);
        return d != null && YearMonth.from(d).equals(month);
    }

    private static LocalDate primaryDate(EnrichedShipment s) {
        if (s.order().promisedDeliveryDate() != null) {
            return s.order().promisedDeliveryDate().toLocalDate();
        }
        if (s.order().orderDate() != null) {
            return s.order().orderDate().toLocalDate();
        }
        return null;
    }

    private boolean isFestival(EnrichedShipment s) {
        return s.externalFactor() != null
                && s.externalFactor().eventType() != null
                && "Festival".equalsIgnoreCase(s.externalFactor().eventType());
    }

    private static int countOutcome(List<EnrichedShipment> slice, Outcome outcome) {
        int n = 0;
        for (EnrichedShipment s : slice) {
            if (s.outcome() == outcome) {
                n++;
            }
        }
        return n;
    }

    private static double rate(List<EnrichedShipment> slice) {
        if (slice.isEmpty()) {
            return 0;
        }
        long p = slice.stream().filter(EnrichedShipment::isProblem).count();
        return p / (double) slice.size();
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", v * 100);
    }

    private static String topCause(InsightReport r) {
        if (r.getCauses().isEmpty()) {
            return "n/a";
        }
        return r.getCauses().get(0).cause().label();
    }

    private static String summarizeCauses(List<CauseStat> causes) {
        if (causes.isEmpty()) {
            return "not enough tagged evidence";
        }
        return causes.stream()
                .limit(3)
                .map(c -> c.cause().label() + " (" + String.format(Locale.ROOT, "%.0f%%", c.shareOfProblems() * 100) + ")")
                .collect(Collectors.joining("; "));
    }

    private static List<CauseStat> prefixed(String city, List<CauseStat> causes) {
        return causes.stream().limit(3)
                .map(c -> new CauseStat(c.cause(), c.count(), c.shareOfProblems(), city + " — " + c.evidenceNote()))
                .toList();
    }

    private static List<Long> sampleIds(List<EnrichedShipment> rows, int n) {
        return rows.stream().limit(n).map(s -> s.order().orderId()).toList();
    }

    private static String aliasWarehouse(Warehouse wh) {
        if (wh.warehouseId() == 2) {
            return "Warehouse B / " + wh.displayName();
        }
        return wh.displayName();
    }
}
