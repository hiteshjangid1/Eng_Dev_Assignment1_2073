package com.logistics.rca.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.rca.csv.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

@Component
public class AiQuestionRouter {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionRouter.class);

    private static final String SYSTEM = """
            Map a logistics operations question to one structured intent.
            Dataset calendar: orders from 2025-01-01 to 2025-09-12. Operational as-of date is 2025-09-12.
            "Yesterday" in this dataset means 2025-01-24 for New Delhi if the user does not specify another date
            (that is the highest-signal demo day). "Last month" means 2025-08. Warehouse B is warehouseId 2.
            Client X default is clientId 409. Client Y / 20000 extra orders uses similarClientId 118.
            Return JSON only:
            {"intent":"CITY_DELAYS|CLIENT_FAILURES|WAREHOUSE_FAILURES|CITY_COMPARE|FESTIVAL|CAPACITY_RISK",
             "city":"New Delhi","cityB":"Ahmedabad","date":"2025-01-24","from":"2025-08-10","to":"2025-08-16",
             "yearMonth":"2025-08","clientId":409,"warehouseId":2,"similarClientId":118,"extraMonthlyOrders":20000}
            Omit unused fields. Use the defaults above when the user is vague.
            """;

    private final AiRuntime runtime;
    private final OpenAiClient client;
    private final ObjectMapper mapper;
    private final DataStore store;

    public AiQuestionRouter(AiRuntime runtime, OpenAiClient client, ObjectMapper mapper, DataStore store) {
        this.runtime = runtime;
        this.client = client;
        this.mapper = mapper;
        this.store = store;
    }

    public RoutedQuestion route(String question) {
        if (runtime.shouldCallLlm()) {
            try {
                String content = client.complete(SYSTEM, question);
                JsonNode json = mapper.readTree(AiNarrativeService.stripFences(content));
                RoutedQuestion routed = fromJson(json);
                if (routed.intent() != null) {
                    return routed;
                }
            } catch (Exception e) {
                log.warn("LLM router failed, using keyword fallback: {}", e.getMessage());
            }
        }
        return heuristic(question);
    }

    private RoutedQuestion fromJson(JsonNode n) {
        Intent intent = parseIntent(n.path("intent").asText(""));
        return new RoutedQuestion(
                intent,
                text(n, "city"),
                text(n, "cityB"),
                date(n, "date"),
                date(n, "from"),
                date(n, "to"),
                yearMonth(n, "yearMonth"),
                n.hasNonNull("clientId") ? n.get("clientId").asLong() : null,
                n.hasNonNull("warehouseId") ? n.get("warehouseId").asLong() : null,
                n.hasNonNull("similarClientId") ? n.get("similarClientId").asLong() : null,
                n.hasNonNull("extraMonthlyOrders") ? n.get("extraMonthlyOrders").asInt() : null
        );
    }

    RoutedQuestion heuristic(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (q.contains("festival") || q.contains("diwali") || q.contains("holiday period")) {
            return new RoutedQuestion(Intent.FESTIVAL, null, null, null,
                    LocalDate.of(2025, 1, 1), store.asOfDate(), null, null, null, null, null);
        }
        if (q.contains("onboard") || q.contains("20,000") || q.contains("20000") || q.contains("client y")) {
            return new RoutedQuestion(Intent.CAPACITY_RISK, null, null, null, null, null, null,
                    null, null, 118L, 20_000);
        }
        if (q.contains("compare") || q.contains(" vs ") || q.contains("versus") || q.contains("city a")) {
            return new RoutedQuestion(Intent.CITY_COMPARE, "New Delhi", "Ahmedabad", null, null, null,
                    YearMonth.of(2025, 8), null, null, null, null);
        }
        if (q.contains("warehouse")) {
            return new RoutedQuestion(Intent.WAREHOUSE_FAILURES, null, null, null, null, null,
                    YearMonth.of(2025, 8), null, 2L, null, null);
        }
        if (q.contains("client")) {
            return new RoutedQuestion(Intent.CLIENT_FAILURES, null, null, null,
                    LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 16), null, 409L, null, null, null);
        }
        String city = detectCity(q);
        LocalDate date = LocalDate.of(2025, 1, 24);
        return new RoutedQuestion(Intent.CITY_DELAYS, city, null, date, null, null, null, null, null, null, null);
    }

    private String detectCity(String q) {
        for (String city : new String[]{"New Delhi", "Ahmedabad", "Chennai", "Bengaluru", "Coimbatore",
                "Mysuru", "Surat", "Nagpur", "Mumbai", "Pune"}) {
            if (q.contains(city.toLowerCase(Locale.ROOT))) {
                return city;
            }
        }
        return "New Delhi";
    }

    private static Intent parseIntent(String raw) {
        try {
            return Intent.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Intent.CITY_DELAYS;
        }
    }

    private static String text(JsonNode n, String field) {
        String v = n.path(field).asText(null);
        return v == null || v.isBlank() ? null : v;
    }

    private static LocalDate date(JsonNode n, String field) {
        String v = text(n, field);
        return v == null ? null : LocalDate.parse(v);
    }

    private static YearMonth yearMonth(JsonNode n, String field) {
        String v = text(n, field);
        return v == null ? null : YearMonth.parse(v);
    }

    public enum Intent {
        CITY_DELAYS, CLIENT_FAILURES, WAREHOUSE_FAILURES, CITY_COMPARE, FESTIVAL, CAPACITY_RISK
    }

    public record RoutedQuestion(
            Intent intent,
            String city,
            String cityB,
            LocalDate date,
            LocalDate from,
            LocalDate to,
            YearMonth yearMonth,
            Long clientId,
            Long warehouseId,
            Long similarClientId,
            Integer extraMonthlyOrders
    ) {
    }
}
