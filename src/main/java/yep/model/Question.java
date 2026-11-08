package yep.model;

import java.util.List;

public class Question {
    private final String text;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;

    public Question(String text, List<String> options, int correctIndex, String explanation) {
        this.text = text;
        this.options = List.copyOf(options);
        this.correctIndex = correctIndex;
        this.explanation = explanation;
    }

    public boolean isCorrect(Integer answerIndex) {
        return answerIndex != null && answerIndex == correctIndex;
    }

    public String getText() { return text; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
}
