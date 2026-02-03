package com.datacentric.timesense.controller.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarMatrixData {
    public static final class Views {

        public interface Basic {
        }

    }

    public static class Entry {
        private String status;
        private String type;
        private double count;

        public Entry(String status, String type, double count) {
            this.status = status;
            this.type = type;
            this.count = count;
        }

        @JsonView(Views.Basic.class)
        public String getStatus() {
            return status;
        }

        @JsonView(Views.Basic.class)
        public String getType() {
            return type;
        }

        @JsonView(Views.Basic.class)
        public double getCount() {
            return count;
        }
    }

    // Map key: "STATUS|TYPE", value: count
    private final Map<String, Double> entryCounts = new HashMap<>();

    public void incrementEntry(String status, String type, double value) {
        String key = status.toUpperCase() + "|" + type.toUpperCase();
        entryCounts.merge(key, value, Double::sum);
    }

    @JsonView(Views.Basic.class)
    @JsonProperty("entries")
    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> e : entryCounts.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            entries.add(new Entry(parts[0], parts[1], e.getValue()));
        }
        return entries;
    }

    @JsonView(Views.Basic.class)
    @JsonProperty("total")
    public double getTotal() {
        return entryCounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }
}
