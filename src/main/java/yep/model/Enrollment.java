package yep.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class Enrollment {
    private final int enrollmentId;
    private final YouthParticipant user;
    private final Cohort cohort;
    private String status; // ACTIVE or COMPLETED
    private int completionPct;
    private final LocalDate enrolledOn;
    /** Lessons the participant has marked as done. */
    private final Set<Integer> completedLessonIds = new LinkedHashSet<>();

    public Enrollment(int enrollmentId, YouthParticipant user, Cohort cohort, LocalDate enrolledOn) {
        this.enrollmentId = enrollmentId;
        this.user = user;
        this.cohort = cohort;
        this.status = "ACTIVE";
        this.completionPct = 0;
        this.enrolledOn = enrolledOn;
    }

    public void updateProgress(int completionPct) {
        this.completionPct = Math.max(0, Math.min(100, completionPct));
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completionPct = 100;
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public int getEnrollmentId() { return enrollmentId; }
    public YouthParticipant getUser() { return user; }
    public Cohort getCohort() { return cohort; }
    public String getStatus() { return status; }
    public int getCompletionPct() { return completionPct; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
    public Set<Integer> getCompletedLessonIds() { return completedLessonIds; }
}
