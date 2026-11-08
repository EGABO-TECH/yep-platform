package yep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A report with a table of rows and some headline figures. Can be exported as CSV. */
public class Report {
    private final String title;
    private final List<String> columns;
    private final List<List<String>> rows = new ArrayList<>();
    private final Map<String, Object> totals = new LinkedHashMap<>();
    private final LocalDateTime generatedOn = LocalDateTime.now();

    public Report(String title, List<String> columns) {
        this.title = title;
        this.columns = List.copyOf(columns);
    }

    public void addRow(String... values) {
        rows.add(List.of(values));
    }

    public void addTotal(String label, Object value) {
        totals.put(label, value);
    }

    /** export() / WRITE_CSV(report). */
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(csvLine(columns));
        for (List<String> row : rows) {
            sb.append(csvLine(row));
        }
        if (!totals.isEmpty()) {
            sb.append("\n");
            totals.forEach((k, v) -> sb.append(csvLine(List.of(k, String.valueOf(v)))));
        }
        return sb.toString();
    }

    private static String csvLine(List<String> values) {
        List<String> escaped = new ArrayList<>();
        for (String v : values) {
            String s = v == null ? "" : v;
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            escaped.add(s);
        }
        return String.join(",", escaped) + "\n";
    }

    public String getTitle() { return title; }
    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }
    public Map<String, Object> getTotals() { return totals; }
    public LocalDateTime getGeneratedOn() { return generatedOn; }
}
