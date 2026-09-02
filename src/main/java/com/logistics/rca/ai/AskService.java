package com.logistics.rca.ai;

import com.logistics.rca.analysis.InsightService;
import com.logistics.rca.domain.InsightReport;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class AskService {

    private final AiQuestionRouter router;
    private final InsightService insights;

    public AskService(AiQuestionRouter router, InsightService insights) {
        this.router = router;
        this.insights = insights;
    }

    public AskResult ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        AiQuestionRouter.RoutedQuestion routed = router.route(question.trim());
        InsightReport report = switch (routed.intent()) {
            case CITY_DELAYS -> insights.cityDelays(
                    or(routed.city(), "New Delhi"),
                    or(routed.date(), LocalDate.of(2025, 1, 24)));
            case CLIENT_FAILURES -> insights.clientFailures(
                    or(routed.clientId(), 409L),
                    or(routed.from(), LocalDate.of(2025, 8, 10)),
                    or(routed.to(), LocalDate.of(2025, 8, 16)));
            case WAREHOUSE_FAILURES -> insights.warehouseFailures(
                    or(routed.warehouseId(), 2L),
                    or(routed.yearMonth(), YearMonth.of(2025, 8)));
            case CITY_COMPARE -> insights.compareCities(
                    or(routed.city(), "New Delhi"),
                    or(routed.cityB(), "Ahmedabad"),
                    or(routed.yearMonth(), YearMonth.of(2025, 8)));
            case FESTIVAL -> insights.festivalPeriod(
                    or(routed.from(), LocalDate.of(2025, 1, 1)),
                    or(routed.to(), LocalDate.of(2025, 9, 12)));
            case CAPACITY_RISK -> insights.onboardRisk(
                    or(routed.similarClientId(), 118L),
                    or(routed.extraMonthlyOrders(), 20_000));
        };
        return new AskResult(question.trim(), routed.intent().name(), report);
    }

    private static String or(String v, String d) {
        return v == null || v.isBlank() ? d : v;
    }

    private static LocalDate or(LocalDate v, LocalDate d) {
        return v == null ? d : v;
    }

    private static YearMonth or(YearMonth v, YearMonth d) {
        return v == null ? d : v;
    }

    private static long or(Long v, long d) {
        return v == null ? d : v;
    }

    private static int or(Integer v, int d) {
        return v == null ? d : v;
    }

    public record AskResult(String question, String intent, InsightReport report) {
    }
}
