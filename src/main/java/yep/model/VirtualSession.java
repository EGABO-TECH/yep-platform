package yep.model;

import java.time.LocalDateTime;

/** A live Virtual Exchange session hosted by a facilitator. */
public class VirtualSession {
    private final int sessionId;
    private final LearningTrack track;
    private final String title;
    private final LocalDateTime scheduledAt;
    private final int durationMins;
    private final String meetingLink;
    private final Facilitator host;
    private final String countries;

    public VirtualSession(int sessionId, LearningTrack track, String title, LocalDateTime scheduledAt,
                          int durationMins, String meetingLink, Facilitator host, String countries) {
        this.sessionId = sessionId;
        this.track = track;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.durationMins = durationMins;
        this.meetingLink = meetingLink;
        this.host = host;
        this.countries = countries;
    }

    public int getSessionId() { return sessionId; }
    public LearningTrack getTrack() { return track; }
    public String getTitle() { return title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public int getDurationMins() { return durationMins; }
    public String getMeetingLink() { return meetingLink; }
    public Facilitator getHost() { return host; }
    public String getCountries() { return countries; }
}
