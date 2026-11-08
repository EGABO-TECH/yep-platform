package yep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Level Two team project (ImpactProject in the class diagram, Project in the pseudo-code). */
public class ImpactProject implements Scorable {
    private final int projectId;
    private final Team team;
    private String title;
    private String description;
    private String submissionStatus; // DRAFT, SUBMITTED, COMPLETED
    private double score;
    private LocalDateTime submittedAt;
    private final List<Milestone> milestones = new ArrayList<>();

    public ImpactProject(int projectId, Team team, String title, String description) {
        this.projectId = projectId;
        this.team = team;
        this.title = title;
        this.description = description;
        this.submissionStatus = "DRAFT";
    }

    @Override
    public double getPercentage() {
        return score;
    }

    @Override
    public boolean isPassed() {
        return score >= 50;
    }

    public void submit() {
        submissionStatus = "SUBMITTED";
        submittedAt = LocalDateTime.now();
    }

    /** The milestone before the given one, or null if it is the first. */
    public Milestone previousOf(Milestone milestone) {
        int index = milestones.indexOf(milestone);
        return index > 0 ? milestones.get(index - 1) : null;
    }

    public long countApproved() {
        return milestones.stream().filter(Milestone::isApproved).count();
    }

    public void updateStatus(String status) { this.submissionStatus = status; }
    public void setScore(double score) { this.score = score; }

    public int getProjectId() { return projectId; }
    public Team getTeam() { return team; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSubmissionStatus() { return submissionStatus; }
    public double getScore() { return score; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public List<Milestone> getMilestones() { return milestones; }
}
