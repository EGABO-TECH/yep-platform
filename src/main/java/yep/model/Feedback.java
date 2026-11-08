package yep.model;

import java.time.LocalDateTime;

public class Feedback {
    private final int feedbackId;
    private final ImpactProject project;
    private final Milestone milestone;
    private final User author;
    private final String comment;
    private final int rating;
    private final LocalDateTime givenOn;

    public Feedback(int feedbackId, ImpactProject project, Milestone milestone, User author,
                    String comment, int rating, LocalDateTime givenOn) {
        this.feedbackId = feedbackId;
        this.project = project;
        this.milestone = milestone;
        this.author = author;
        this.comment = comment;
        this.rating = rating;
        this.givenOn = givenOn;
    }

    public int getFeedbackId() { return feedbackId; }
    public ImpactProject getProject() { return project; }
    public Milestone getMilestone() { return milestone; }
    public User getAuthor() { return author; }
    public String getComment() { return comment; }
    public int getRating() { return rating; }
    public LocalDateTime getGivenOn() { return givenOn; }
}
