package yep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** AssessmentResult in the class diagram, Submission in the pseudo-code. */
public class Submission implements Scorable {
    private final int submissionId;
    private final Assessment assessment;
    private final YouthParticipant user;
    private final List<Integer> answers;
    private final String writtenAnswer;
    private Double score;
    private String feedback;
    private final LocalDateTime submittedAt;
    private User gradedBy;

    public Submission(int submissionId, Assessment assessment, YouthParticipant user,
                      List<Integer> answers, String writtenAnswer, LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.assessment = assessment;
        this.user = user;
        this.answers = answers == null ? new ArrayList<>() : new ArrayList<>(answers);
        this.writtenAnswer = writtenAnswer;
        this.submittedAt = submittedAt;
    }

    @Override
    public double getPercentage() {
        if (score == null) {
            return 0;
        }
        return (score / assessment.getMaxScore()) * 100;
    }

    @Override
    public boolean isPassed() {
        return score != null && getPercentage() >= assessment.getPassMarkPercent();
    }

    public boolean isGraded() {
        return score != null;
    }

    public void grade(double score, String feedback, User gradedBy) {
        this.score = score;
        this.feedback = feedback;
        this.gradedBy = gradedBy;
    }

    public int getSubmissionId() { return submissionId; }
    public Assessment getAssessment() { return assessment; }
    public YouthParticipant getUser() { return user; }
    public List<Integer> getAnswers() { return answers; }
    public String getWrittenAnswer() { return writtenAnswer; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getFeedback() { return feedback; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public User getGradedBy() { return gradedBy; }
}
