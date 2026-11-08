package yep.model;

import java.time.LocalDateTime;

/** Message in the class diagram: a notification sent to one user. */
public class Notification {
    private final int messageId;
    private final User user;
    private final String content;
    private final String link;
    private final LocalDateTime sentAt;
    private boolean read;

    public Notification(int messageId, User user, String content, String link, LocalDateTime sentAt) {
        this.messageId = messageId;
        this.user = user;
        this.content = content;
        this.link = link;
        this.sentAt = sentAt;
    }

    public void markRead() { read = true; }

    public int getMessageId() { return messageId; }
    public User getUser() { return user; }
    public String getContent() { return content; }
    public String getLink() { return link; }
    public LocalDateTime getSentAt() { return sentAt; }
    public boolean isRead() { return read; }
}
