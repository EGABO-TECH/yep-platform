package yep.model;

import java.time.LocalDateTime;

public class DiscussionPost {
    private final int postId;
    private final Lesson lesson;
    private final User author;
    private final String body;
    private final LocalDateTime postedAt;
    private boolean flagged;
    private boolean hidden;

    public DiscussionPost(int postId, Lesson lesson, User author, String body, LocalDateTime postedAt) {
        this.postId = postId;
        this.lesson = lesson;
        this.author = author;
        this.body = body;
        this.postedAt = postedAt;
    }

    public void flag() { flagged = true; }
    public void hide() { hidden = true; flagged = false; }
    public void restore() { hidden = false; flagged = false; }

    public int getPostId() { return postId; }
    public Lesson getLesson() { return lesson; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public boolean isFlagged() { return flagged; }
    public boolean isHidden() { return hidden; }
}
