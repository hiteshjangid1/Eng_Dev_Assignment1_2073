package com.logistics.rca.analysis;

import com.logistics.rca.domain.Cause;
import com.logistics.rca.domain.CauseStat;
import com.logistics.rca.domain.EnrichedShipment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RecommendationCatalog {

    public List<String> forCauses(List<CauseStat> causes, String contextHint) {
        List<String> recs = new ArrayList<>();
        for (CauseStat stat : causes) {
            recs.add(line(stat.cause(), contextHint));
            if (recs.size() >= 5) {
                break;
            }
        }
        if (recs.isEmpty()) {
            recs.add("Keep joining order, warehouse, fleet, and external feeds daily so empty slices are investigated as data-coverage issues, not as zero risk.");
        }
        return recs;
    }

    public Map<Cause, String> playbook() {
        Map<Cause, String> map = new EnumMap<>(Cause.class);
        for (Cause c : Cause.values()) {
            map.put(c, line(c, null));
        }
        return map;
    }

    private String line(Cause cause, String hint) {
        String suffix = hint == null || hint.isBlank() ? "" : " (" + hint + ")";
        return switch (cause) {
            case STOCKOUT ->
                    "Raise safety stock and block promise-to-customer until pick confirmation for high-risk SKUs" + suffix + ".";
            case WAREHOUSE_DELAY ->
                    "Review dispatch cut-offs and add packing shifts for warehouses that miss SLA dwell time" + suffix + ".";
            case SLOW_PACKING ->
                    "Rebalance pack-station staffing to demand peaks; time-box pick-to-pack KPIs" + suffix + ".";
            case SYSTEM_ISSUE ->
                    "Treat WMS outages as a P1 ops risk: failover checklist and manual dispatch fallback" + suffix + ".";
            case TRAFFIC ->
                    "Shift delivery windows away from congested hours and add rider buffer in affected cities" + suffix + ".";
            case WEATHER ->
                    "Apply weather-aware ETAs and pause non-critical COD attempts during rain/fog alerts" + suffix + ".";
            case ADDRESS ->
                    "Mandate address verification, geocode, and landmark capture at checkout; flag failed geocodes before dispatch" + suffix + ".";
            case VEHICLE_BREAKDOWN ->
                    "Stage spare vehicles on high-volume routes and auto-reassign when GPS stall + breakdown notes appear" + suffix + ".";
            case FESTIVAL ->
                    "Pre-build festival playbook: surge roster, inventory pre-positioning, and relaxed SLA on non-priority SKUs" + suffix + ".";
            case HOLIDAY ->
                    "Publish holiday capacity calendars to clients and freeze same-day promises on known holiday dates" + suffix + ".";
            case STRIKE ->
                    "Maintain alternate partner mix (in-house + 3PL) so a single strike cannot stall a city" + suffix + ".";
            case CUSTOMER_RETURN ->
                    "Tighten first-attempt windows and confirmation SMS/calls before the rider arrives" + suffix + ".";
            case COMMUNICATION ->
                    "Push live status to customers when delay evidence is tagged; reduce 'no update' complaints" + suffix + ".";
            case SLA_BREACH ->
                    "Recalibrate promised dates using city-level transit time, not a flat N-day SLA" + suffix + ".";
        };
    }
}
