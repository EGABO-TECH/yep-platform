package yep.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Rules the administrator sets for the AI assistant. */
public class AIUsageGuideline {
    private final String version;
    private final List<String> rules;
    private final int dailyLimit;
    private final LocalDate effectiveFrom;

    public AIUsageGuideline(String version, List<String> rules, int dailyLimit, LocalDate effectiveFrom) {
        this.version = version;
        this.rules = new ArrayList<>(rules);
        this.dailyLimit = dailyLimit;
        this.effectiveFrom = effectiveFrom;
    }

    public String getVersion() { return version; }
    public List<String> getRules() { return rules; }
    public int getDailyLimit() { return dailyLimit; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
}
