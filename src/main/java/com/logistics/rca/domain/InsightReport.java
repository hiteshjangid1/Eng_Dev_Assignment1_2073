package com.logistics.rca.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InsightReport {
    private String title;
    private String question;
    private String scope;
    private Map<String, Object> metrics = new LinkedHashMap<>();
    private List<CauseStat> causes = new ArrayList<>();
    private String narrative;
    private List<String> recommendations = new ArrayList<>();
    private List<Long> sampleOrderIds = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public List<CauseStat> getCauses() {
        return causes;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public List<Long> getSampleOrderIds() {
        return sampleOrderIds;
    }

    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("=".repeat(Math.min(80, title.length()))).append("\n");
        if (question != null) {
            sb.append("Question: ").append(question).append("\n");
        }
        sb.append("Scope: ").append(scope).append("\n\n");
        sb.append("Metrics\n");
        metrics.forEach((k, v) -> sb.append("  - ").append(k).append(": ").append(v).append("\n"));
        sb.append("\nRanked causes\n");
        if (causes.isEmpty()) {
            sb.append("  (no tagged causes in this slice)\n");
        } else {
            int i = 1;
            for (CauseStat c : causes) {
                sb.append("  ").append(i++).append(". ").append(c.cause().label())
                        .append(" — ").append(c.count()).append(c.count() == 1 ? " shipment (" : " shipments (")
                        .append(String.format("%.1f%%", c.shareOfProblems() * 100))
                        .append(" of problem set)");
                if (c.evidenceNote() != null && !c.evidenceNote().isBlank()) {
                    sb.append(" [").append(c.evidenceNote()).append("]");
                }
                sb.append("\n");
            }
        }
        sb.append("\nInsight\n").append(narrative).append("\n");
        sb.append("\nRecommendations\n");
        for (int i = 0; i < recommendations.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(recommendations.get(i)).append("\n");
        }
        if (!sampleOrderIds.isEmpty()) {
            sb.append("\nSample order IDs: ").append(sampleOrderIds).append("\n");
        }
        return sb.toString();
    }
}
