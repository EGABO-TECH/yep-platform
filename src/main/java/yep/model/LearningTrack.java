package yep.model;

import java.util.ArrayList;
import java.util.List;

public class LearningTrack {
    private final int trackId;
    private final Programme programme;
    private final String title;
    private final String topic;
    private final String summary;
    private final List<Lesson> lessons = new ArrayList<>();
    private final List<Assessment> assessments = new ArrayList<>();

    public LearningTrack(int trackId, Programme programme, String title, String topic, String summary) {
        this.trackId = trackId;
        this.programme = programme;
        this.title = title;
        this.topic = topic;
        this.summary = summary;
    }

    /** addContent() from the class diagram. */
    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
    }

    public void addAssessment(Assessment assessment) {
        assessments.add(assessment);
    }

    public int getTrackId() { return trackId; }
    public Programme getProgramme() { return programme; }
    public String getTitle() { return title; }
    public String getTopic() { return topic; }
    public String getSummary() { return summary; }
    public List<Lesson> getLessons() { return lessons; }
    public List<Assessment> getAssessments() { return assessments; }
}
