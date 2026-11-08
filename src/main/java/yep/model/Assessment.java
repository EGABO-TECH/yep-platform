package yep.model;

import java.util.ArrayList;
import java.util.List;

public class Assessment {
    public static final String MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    public static final String WRITTEN = "WRITTEN";

    private final int assessmentId;
    private final LearningTrack track;
    private final String title;
    private final String type;
    private final double maxScore;
    private final double passMarkPercent;
    private final int timeLimitMinutes;
    private final String writtenPrompt;
    private final List<Question> questions = new ArrayList<>();

    public Assessment(int assessmentId, LearningTrack track, String title, String type,
                      double maxScore, int timeLimitMinutes, String writtenPrompt) {
        this.assessmentId = assessmentId;
        this.track = track;
        this.title = title;
        this.type = type;
        this.maxScore = maxScore;
        this.passMarkPercent = 50;
        this.timeLimitMinutes = timeLimitMinutes;
        this.writtenPrompt = writtenPrompt;
    }

    public boolean isMultipleChoice() {
        return MULTIPLE_CHOICE.equals(type);
    }

    /** AUTO_MARK(assessment, answers): each correct answer earns an equal share of maxScore. */
    public double autoGrade(List<Integer> answers) {
        if (questions.isEmpty()) {
            return 0;
        }
        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            Integer answer = i < answers.size() ? answers.get(i) : null;
            if (questions.get(i).isCorrect(answer)) {
                correct++;
            }
        }
        return Math.round((correct * maxScore / questions.size()) * 10.0) / 10.0;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public int getAssessmentId() { return assessmentId; }
    public LearningTrack getTrack() { return track; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public double getMaxScore() { return maxScore; }
    public double getPassMarkPercent() { return passMarkPercent; }
    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public String getWrittenPrompt() { return writtenPrompt; }
    public List<Question> getQuestions() { return questions; }
}
