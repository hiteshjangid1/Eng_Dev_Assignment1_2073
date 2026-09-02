package com.logistics.rca.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.rca.domain.CauseStat;
import com.logistics.rca.domain.InsightReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AiNarrativeService {

    private static final Logger log = LoggerFactory.getLogger(AiNarrativeService.class);

    private static final String SYSTEM = """
            You are a logistics operations analyst. Rewrite a root-cause briefing for an operations manager.
            Rules:
            - Use ONLY the facts in the JSON. Do not invent causes, cities, warehouses, clients, or percentages.
            - Causes were tagged by a deterministic rules engine from orders, warehouse logs, fleet notes, weather/traffic, and feedback.
            - Write 2-4 short paragraphs: what happened, why (ranked evidence), what to do first.
            - Recommendations must follow from the tagged causes (staffing, windows, address verification, inventory, festival surge, phased onboard).
            - Return JSON only: {"narrative":"...","recommendations":["...","..."]}.
            """;

    private final AiRuntime runtime;
    private final AiProperties properties;
    private final OpenAiClient client;
    private final ObjectMapper mapper;

    public AiNarrativeService(AiRuntime runtime, AiProperties properties, OpenAiClient client, ObjectMapper mapper) {
        this.runtime = runtime;
        this.properties = properties;
        this.client = client;
        this.mapper = mapper;
    }

    public InsightReport enrich(InsightReport report) {
        if (report == null) {
            return null;
        }
        if (report.getRuleBasedNarrative() == null) {
            report.setRuleBasedNarrative(report.getNarrative());
        }
        if (!runtime.shouldCallLlm()) {
            report.setAiGenerated(false);
            report.setAiModel(null);
            return report;
        }
        try {
            String user = mapper.writeValueAsString(payload(report));
            String content = client.complete(SYSTEM, user);
            JsonNode json = mapper.readTree(stripFences(content));
            String narrative = json.path("narrative").asText(null);
            if (narrative != null && !narrative.isBlank()) {
                report.setNarrative(narrative.trim());
            }
            JsonNode recs = json.path("recommendations");
            if (recs.isArray() && recs.size() > 0) {
                List<String> next = new ArrayList<>();
                recs.forEach(n -> {
                    String t = n.asText("");
                    if (!t.isBlank()) {
                        next.add(t.trim());
                    }
                });
                if (!next.isEmpty()) {
                    report.getRecommendations().clear();
                    report.getRecommendations().addAll(next);
                }
            }
            report.setAiGenerated(true);
            report.setAiModel(properties.getModel());
        } catch (Exception e) {
            log.warn("AI narrative skipped, keeping rule-based text: {}", e.getMessage());
            report.setAiGenerated(false);
            report.setAiModel(null);
        }
        return report;
    }

    private static Object payload(InsightReport report) {
        return java.util.Map.of(
                "question", nullToEmpty(report.getQuestion()),
                "scope", nullToEmpty(report.getScope()),
                "metrics", report.getMetrics(),
                "causes", report.getCauses().stream().map(AiNarrativeService::causeRow).toList(),
                "sampleOrderIds", report.getSampleOrderIds(),
                "complaintSamples", report.getComplaintSamples(),
                "draftNarrative", nullToEmpty(report.getRuleBasedNarrative()),
                "draftRecommendations", report.getRecommendations()
        );
    }

    private static java.util.Map<String, Object> causeRow(CauseStat c) {
        return java.util.Map.of(
                "cause", c.cause().label(),
                "count", c.count(),
                "shareOfProblems", String.format(Locale.ROOT, "%.1f%%", c.shareOfProblems() * 100),
                "evidence", c.evidenceNote() == null ? "" : c.evidenceNote()
        );
    }

    static String stripFences(String content) {
        String t = content.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
