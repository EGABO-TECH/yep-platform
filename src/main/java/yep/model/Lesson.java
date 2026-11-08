package yep.model;

import java.util.ArrayList;
import java.util.List;

/** LearningContent in the class diagram: one lesson inside a learning track. */
public class Lesson {
    private final int contentId;
    private final LearningTrack track;
    private final String title;
    private final List<String> paragraphs;
    private final String videoTitle;
    private final double videoSizeMb;
    private final int minutes;

    public Lesson(int contentId, LearningTrack track, String title, List<String> paragraphs,
                  String videoTitle, double videoSizeMb, int minutes) {
        this.contentId = contentId;
        this.track = track;
        this.title = title;
        this.paragraphs = new ArrayList<>(paragraphs);
        this.videoTitle = videoTitle;
        this.videoSizeMb = videoSizeMb;
        this.minutes = minutes;
    }

    /** LIGHT_VERSION(): text only, no video. Used when the data saver is on. */
    public String getLightText() {
        return String.join("\n\n", paragraphs);
    }

    public int getContentId() { return contentId; }
    public LearningTrack getTrack() { return track; }
    public String getTitle() { return title; }
    public List<String> getParagraphs() { return paragraphs; }
    public String getVideoTitle() { return videoTitle; }
    public double getVideoSizeMb() { return videoSizeMb; }
    public int getMinutes() { return minutes; }
}
