package yep.model;

import java.util.ArrayList;
import java.util.List;

public class Programme {
    private final int programmeId;
    private final String name;
    private final String level; // "One" or "Two"
    private String status;
    private final List<Cohort> cohorts = new ArrayList<>();
    private final List<LearningTrack> tracks = new ArrayList<>();

    public Programme(int programmeId, String name, String level) {
        this.programmeId = programmeId;
        this.name = name;
        this.level = level;
        this.status = "ACTIVE";
    }

    /** Every assessment in every track of this programme. */
    public List<Assessment> getAllAssessments() {
        List<Assessment> all = new ArrayList<>();
        for (LearningTrack track : tracks) {
            all.addAll(track.getAssessments());
        }
        return all;
    }

    public int getProgrammeId() { return programmeId; }
    public String getName() { return name; }
    public String getLevel() { return level; }
    public String getStatus() { return status; }
    public List<Cohort> getCohorts() { return cohorts; }
    public List<LearningTrack> getTracks() { return tracks; }
}
