package com.logistics.rca.analysis;

import com.logistics.rca.domain.Cause;
import com.logistics.rca.domain.EnrichedShipment;
import com.logistics.rca.domain.ExternalFactor;
import com.logistics.rca.domain.Feedback;
import com.logistics.rca.domain.FleetLog;
import com.logistics.rca.domain.OrderRecord;
import com.logistics.rca.domain.Outcome;
import com.logistics.rca.domain.WarehouseLog;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;

@Component
public class CauseEngine {

    public Outcome outcome(EnrichedShipment shipment, LocalDate asOf) {
        OrderRecord o = shipment.order();
        String status = o.status() == null ? "" : o.status();
        boolean lateVsPromise = o.actualDeliveryDate() != null
                && o.promisedDeliveryDate() != null
                && o.actualDeliveryDate().isAfter(o.promisedDeliveryDate());
        if ("Failed".equalsIgnoreCase(status)) {
            return Outcome.FAILED;
        }
        if ("Returned".equalsIgnoreCase(status)) {
            return Outcome.RETURNED;
        }
        if ("Delivered".equalsIgnoreCase(status)) {
            return lateVsPromise ? Outcome.DELAYED : Outcome.ON_TIME;
        }
        if (o.promisedDeliveryDate() != null
                && o.promisedDeliveryDate().toLocalDate().isBefore(asOf)
                && ("Pending".equalsIgnoreCase(status) || "In-Transit".equalsIgnoreCase(status))) {
            return Outcome.OPEN_LATE;
        }
        return Outcome.IN_PROGRESS;
    }

    public EnumSet<Cause> classify(EnrichedShipment shipment, Outcome outcome) {
        EnumSet<Cause> causes = EnumSet.noneOf(Cause.class);
        OrderRecord o = shipment.order();
        addFromFailureReason(causes, o.failureReason());
        for (WarehouseLog log : shipment.warehouseLogs()) {
            addFromWarehouseNote(causes, log.notes());
        }
        FleetLog fleet = shipment.fleetLog();
        if (fleet != null) {
            addFromFleetNote(causes, fleet.gpsDelayNotes());
        }
        ExternalFactor ext = shipment.externalFactor();
        if (ext != null) {
            addFromExternal(causes, ext);
        }
        for (Feedback fb : shipment.feedback()) {
            addFromFeedback(causes, fb.feedbackText());
        }
        if (outcome == Outcome.RETURNED) {
            causes.add(Cause.CUSTOMER_RETURN);
        }
        if ((outcome == Outcome.DELAYED || outcome == Outcome.OPEN_LATE) && causes.isEmpty()) {
            causes.add(Cause.SLA_BREACH);
        }
        return causes;
    }

    private void addFromFailureReason(EnumSet<Cause> causes, String reason) {
        String r = norm(reason);
        if (r.isEmpty()) {
            return;
        }
        if (r.contains("stockout")) {
            causes.add(Cause.STOCKOUT);
        }
        if (r.contains("warehouse")) {
            causes.add(Cause.WAREHOUSE_DELAY);
        }
        if (r.contains("incorrect address") || r.contains("address")) {
            causes.add(Cause.ADDRESS);
        }
        if (r.contains("weather")) {
            causes.add(Cause.WEATHER);
        }
        if (r.contains("traffic")) {
            causes.add(Cause.TRAFFIC);
        }
    }

    private void addFromWarehouseNote(EnumSet<Cause> causes, String notes) {
        String r = norm(notes);
        if (r.contains("stock")) {
            causes.add(Cause.STOCKOUT);
        }
        if (r.contains("slow packing")) {
            causes.add(Cause.SLOW_PACKING);
            causes.add(Cause.WAREHOUSE_DELAY);
        }
        if (r.contains("system")) {
            causes.add(Cause.SYSTEM_ISSUE);
            causes.add(Cause.WAREHOUSE_DELAY);
        }
    }

    private void addFromFleetNote(EnumSet<Cause> causes, String notes) {
        String r = norm(notes);
        if (r.contains("address")) {
            causes.add(Cause.ADDRESS);
        }
        if (r.contains("congestion")) {
            causes.add(Cause.TRAFFIC);
        }
        if (r.contains("breakdown")) {
            causes.add(Cause.VEHICLE_BREAKDOWN);
        }
    }

    private void addFromExternal(EnumSet<Cause> causes, ExternalFactor ext) {
        if ("Heavy".equalsIgnoreCase(nullToEmpty(ext.trafficCondition()))) {
            causes.add(Cause.TRAFFIC);
        }
        String weather = nullToEmpty(ext.weatherCondition());
        if ("Rain".equalsIgnoreCase(weather) || "Fog".equalsIgnoreCase(weather)) {
            causes.add(Cause.WEATHER);
        }
        String event = nullToEmpty(ext.eventType());
        if ("Festival".equalsIgnoreCase(event)) {
            causes.add(Cause.FESTIVAL);
        }
        if ("Holiday".equalsIgnoreCase(event)) {
            causes.add(Cause.HOLIDAY);
        }
        if ("Strike".equalsIgnoreCase(event)) {
            causes.add(Cause.STRIKE);
        }
    }

    private void addFromFeedback(EnumSet<Cause> causes, String text) {
        String r = norm(text);
        if (r.contains("wrong address") || r.contains("couldn't find") || r.contains("could not find")) {
            causes.add(Cause.ADDRESS);
        }
        if (r.contains("stock")) {
            causes.add(Cause.STOCKOUT);
        }
        if (r.contains("no update")) {
            causes.add(Cause.COMMUNICATION);
        }
    }

    private static String norm(String s) {
        return nullToEmpty(s).toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
