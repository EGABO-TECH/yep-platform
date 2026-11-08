package yep.model;

import java.time.LocalDateTime;

public class Mentorship {
    private final int mentorshipId;
    private final Mentor mentor;
    private final Team team;
    private String status; // ACTIVE or ENDED
    private final LocalDateTime startedAt;

    public Mentorship(int mentorshipId, Mentor mentor, Team team, LocalDateTime startedAt) {
        this.mentorshipId = mentorshipId;
        this.mentor = mentor;
        this.team = team;
        this.status = "ACTIVE";
        this.startedAt = startedAt;
    }

    public boolean isActive() { return "ACTIVE".equals(status); }
    public void end() { status = "ENDED"; }

    public int getMentorshipId() { return mentorshipId; }
    public Mentor getMentor() { return mentor; }
    public Team getTeam() { return team; }
    public String getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
}
