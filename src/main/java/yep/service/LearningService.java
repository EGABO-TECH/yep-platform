package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Section 3 of the pseudo-code: Learning Management. */
public class LearningService {
    /** CONTAINS_BLOCKED_WORDS(message): words that break the community rules. */
    private static final List<String> BLOCKED_WORDS = List.of("idiot", "stupid", "hate you", "scam link", "fool");

    private final DataStore store;
    private final AuthService auth;
    private final NotificationService notifications;
    private final AuditService audit;

    public LearningService(DataStore store, AuthService auth, NotificationService notifications, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** enrol(participant, cohortId). */
    public Enrollment enrol(User user, int cohortId) {
        auth.checkPermission(user, Permission.ENROL);
        YouthParticipant participant = (YouthParticipant) user;
        Cohort cohort = DataStore.find(store.cohorts, c -> c.getCohortId() == cohortId)
                .orElseThrow(() -> YepException.notFound("Cohort"));
        if (cohort.isClosed(LocalDate.now())) {
            throw new YepException("This cohort has closed");
        }
        boolean already = store.enrollments.stream().anyMatch(e -> e.getUser().getUserId() == participant.getUserId()
                && e.getCohort().getCohortId() == cohortId);
        if (already) {
            throw new YepException("Already enrolled");
        }
        Enrollment enrollment = new Enrollment(store.newId(), participant, cohort, LocalDate.now());
        store.enrollments.add(enrollment);
        notifications.send(participant, "Welcome to " + cohort.getName() + ". Your first lesson is ready.", "#/learn");
        return enrollment;
    }

    public Optional<Enrollment> enrollmentFor(User user, Programme programme) {
        return DataStore.find(store.enrollments, e -> e.getUser().getUserId() == user.getUserId()
                && e.getCohort().getProgramme().getProgrammeId() == programme.getProgrammeId());
    }

    /** openLesson(participant, trackId, slowConnection): checks enrolment before showing content. */
    public Lesson openLesson(User user, int lessonId) {
        Lesson lesson = findLesson(lessonId);
        if (user instanceof YouthParticipant) {
            if (enrollmentFor(user, lesson.getTrack().getProgramme()).isEmpty()) {
                throw new YepException("Enrol in this programme first");
            }
        }
        return lesson;
    }

    public void markLessonDone(User user, int lessonId) {
        Lesson lesson = openLesson(user, lessonId);
        Enrollment enrollment = enrollmentFor(user, lesson.getTrack().getProgramme())
                .orElseThrow(() -> new YepException("Enrol in this programme first"));
        enrollment.getCompletedLessonIds().add(lessonId);
    }

    /** Per-track progress shown on the dashboard: lessons done plus assessments passed. */
    public int trackProgress(User user, LearningTrack track) {
        Optional<Enrollment> enrollment = enrollmentFor(user, track.getProgramme());
        if (enrollment.isEmpty()) {
            return 0;
        }
        int total = track.getLessons().size() + track.getAssessments().size();
        if (total == 0) {
            return 0;
        }
        long done = track.getLessons().stream()
                .filter(l -> enrollment.get().getCompletedLessonIds().contains(l.getContentId())).count();
        long passed = track.getAssessments().stream()
                .filter(a -> store.submissionOf(user, a).map(Submission::isPassed).orElse(false)).count();
        return (int) Math.round((done + passed) * 100.0 / total);
    }

    /** postDiscussion(user, lessonId, message). */
    public DiscussionPost postDiscussion(User user, int lessonId, String message) {
        if (message == null || message.isBlank()) {
            throw new YepException("Message cannot be empty");
        }
        if (message.length() > 600) {
            throw new YepException("Keep your comment under 600 characters");
        }
        if (containsBlockedWords(message)) {
            audit.record("BLOCKED_POST", user, "Comment rejected by community rules");
            throw new YepException("Your comment breaks the community rules. Please rewrite it respectfully.");
        }
        Lesson lesson = openLesson(user, lessonId);
        DiscussionPost post = new DiscussionPost(store.newId(), lesson, user, message.trim(), LocalDateTime.now());
        store.posts.add(post);
        return post;
    }

    public List<DiscussionPost> postsFor(int lessonId) {
        return store.posts.stream()
                .filter(p -> p.getLesson().getContentId() == lessonId && !p.isHidden())
                .sorted(Comparator.comparing(DiscussionPost::getPostedAt))
                .toList();
    }

    /** flag(): any participant can report a post; facilitators then review it. */
    public void flagPost(User user, int postId) {
        DiscussionPost post = findPost(postId);
        post.flag();
        audit.record("FLAG_POST", user, "Post " + postId + " reported");
    }

    /** moderateDiscussion(): facilitator hides or restores a reported post. */
    public void moderatePost(User user, int postId, boolean hide) {
        auth.checkPermission(user, Permission.MODERATE);
        DiscussionPost post = findPost(postId);
        if (hide) {
            post.hide();
        } else {
            post.restore();
        }
        audit.record(hide ? "HIDE_POST" : "RESTORE_POST", user, "Post " + postId);
    }

    /** addContent(facilitator, trackId, title, contentFile). */
    public Lesson addContent(User user, int trackId, String title, String body, double videoSizeMb) {
        auth.checkPermission(user, Permission.ADD_CONTENT);
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            throw new YepException("Add a title and lesson text");
        }
        LearningTrack track = DataStore.find(store.tracks, t -> t.getTrackId() == trackId)
                .orElseThrow(() -> YepException.notFound("Learning track"));
        List<String> paragraphs = List.of(body.trim().split("\\n\\s*\\n"));
        int minutes = Math.max(3, body.split("\\s+").length / 180 + 2);
        Lesson lesson = new Lesson(store.newId(), track, title.trim(), paragraphs,
                videoSizeMb > 0 ? title.trim() + " (video)" : null, videoSizeMb, minutes);
        track.addLesson(lesson);
        store.lessons.add(lesson);
        audit.record("ADD_CONTENT", user, track.getTitle() + ": " + lesson.getTitle());
        for (Enrollment e : store.enrollments) {
            if (e.getCohort().getProgramme().getProgrammeId() == track.getProgramme().getProgrammeId()) {
                notifications.send(e.getUser(), "New lesson in " + track.getTitle() + ": " + lesson.getTitle(),
                        "#/lesson/" + lesson.getContentId());
            }
        }
        return lesson;
    }

    public Lesson findLesson(int lessonId) {
        return DataStore.find(store.lessons, l -> l.getContentId() == lessonId)
                .orElseThrow(() -> YepException.notFound("Lesson"));
    }

    private DiscussionPost findPost(int postId) {
        return DataStore.find(store.posts, p -> p.getPostId() == postId)
                .orElseThrow(() -> YepException.notFound("Post"));
    }

    static boolean containsBlockedWords(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return BLOCKED_WORDS.stream().anyMatch(lower::contains);
    }
}
