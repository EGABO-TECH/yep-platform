package yep.model;

import java.time.LocalDateTime;

public class AIUsageLog {
    private final int logId;
    private final User user;
    private final String question;
    private final LocalDateTime askedAt;

    public AIUsageLog(int logId, User user, String question, LocalDateTime askedAt) {
        this.logId = logId;
        this.user = user;
        this.question = question;
        this.askedAt = askedAt;
    }

    public int getLogId() { return logId; }
    public User getUser() { return user; }
    public String getQuestion() { return question; }
    public LocalDateTime getAskedAt() { return askedAt; }
}
