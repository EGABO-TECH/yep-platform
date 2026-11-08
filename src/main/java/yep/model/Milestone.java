package yep.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Milestone {
    public static final String OPEN = "OPEN";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String NEEDS_CHANGES = "NEEDS_CHANGES";

    private final int milestoneId;
    private final ImpactProject project;
    private final String title;
    private final LocalDate dueDate;
    private String status;
    private String workNote;
    private String workLink;
    private User submittedBy;
    private LocalDateTime submittedAt;

    public Milestone(int milestoneId, ImpactProject project, String title, LocalDate dueDate) {
        this.milestoneId = milestoneId;
        this.project = project;
        this.title = title;
        this.dueDate = dueDate;
        this.status = OPEN;
    }

    public void submit(User member, String note, String link) {
        this.workNote = note;
        this.workLink = link;
        this.submittedBy = member;
        this.submittedAt = LocalDateTime.now();
        this.status = SUBMITTED;
    }

    public void markApproved() { status = APPROVED; }
    public void markNeedsChanges() { status = NEEDS_CHANGES; }
    public boolean isApproved() { return APPROVED.equals(status); }
    public boolean isSubmitted() { return SUBMITTED.equals(status); }

    public int getMilestoneId() { return milestoneId; }
    public ImpactProject getProject() { return project; }
    public String getTitle() { return title; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public String getWorkNote() { return workNote; }
    public String getWorkLink() { return workLink; }
    public User getSubmittedBy() { return submittedBy; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
