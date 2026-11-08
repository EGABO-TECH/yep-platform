package yep.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** A logged-in session. Expires after 30 minutes without activity. */
public class Session {
    public static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final String token;
    private final User user;
    private LocalDateTime expiresAt;

    public Session(String token, User user) {
        this.token = token;
        this.user = user;
        touch();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void touch() {
        expiresAt = LocalDateTime.now().plus(TIMEOUT);
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
