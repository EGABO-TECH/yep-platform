package yep.model;

import java.time.LocalDate;

public class Cohort {
    private final int cohortId;
    private final Programme programme;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public Cohort(int cohortId, Programme programme, String name, LocalDate startDate, LocalDate endDate) {
        this.cohortId = cohortId;
        this.programme = programme;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean isClosed(LocalDate today) {
        return today.isAfter(endDate);
    }

    public int getCohortId() { return cohortId; }
    public Programme getProgramme() { return programme; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
