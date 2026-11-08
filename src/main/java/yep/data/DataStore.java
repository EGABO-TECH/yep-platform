package yep.data;

import yep.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * In-memory store for every entity (the "SAVE" and "FIND ... WHERE" steps in the pseudo-code).
 * Each list matches one table in the ERD. For a production system this class would be
 * replaced by a database repository with the same methods.
 */
public class DataStore {
    private final AtomicInteger nextId = new AtomicInteger(1000);

    public final List<Organization> organizations = new CopyOnWriteArrayList<>();
    public final List<User> users = new CopyOnWriteArrayList<>();
    public final List<Programme> programmes = new CopyOnWriteArrayList<>();
    public final List<Cohort> cohorts = new CopyOnWriteArrayList<>();
    public final List<LearningTrack> tracks = new CopyOnWriteArrayList<>();
    public final List<Lesson> lessons = new CopyOnWriteArrayList<>();
    public final List<Assessment> assessments = new CopyOnWriteArrayList<>();
    public final List<Enrollment> enrollments = new CopyOnWriteArrayList<>();
    public final List<Submission> submissions = new CopyOnWriteArrayList<>();
    public final List<Certificate> certificates = new CopyOnWriteArrayList<>();
    public final List<VirtualSession> sessions = new CopyOnWriteArrayList<>();
    public final List<DiscussionPost> posts = new CopyOnWriteArrayList<>();
    public final List<Team> teams = new CopyOnWriteArrayList<>();
    public final List<ImpactProject> projects = new CopyOnWriteArrayList<>();
    public final List<Milestone> milestones = new CopyOnWriteArrayList<>();
    public final List<Mentorship> mentorships = new CopyOnWriteArrayList<>();
    public final List<Feedback> feedback = new CopyOnWriteArrayList<>();
    public final List<Portfolio> portfolios = new CopyOnWriteArrayList<>();
    public final List<Notification> notifications = new CopyOnWriteArrayList<>();
    public final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
    public final List<AIUsageLog> aiUsageLogs = new CopyOnWriteArrayList<>();
    public final Map<String, Session> loginSessions = new ConcurrentHashMap<>();
    /** Milestone drafts saved on the server: key = milestoneId, value = draft text. */
    public final Map<Integer, String> milestoneDrafts = new ConcurrentHashMap<>();

    private AIUsageGuideline guideline;

    public int newId() {
        return nextId.incrementAndGet();
    }

    public static <T> Optional<T> find(List<T> list, Predicate<T> where) {
        return list.stream().filter(where).findFirst();
    }

    public static <T> List<T> findAll(List<T> list, Predicate<T> where) {
        return list.stream().filter(where).toList();
    }

    public Optional<User> findUserByEmail(String email) {
        String e = email == null ? "" : email.toLowerCase().trim();
        return find(users, u -> u.getEmail().equals(e));
    }

    public Optional<User> findUser(int id) {
        return find(users, u -> u.getUserId() == id);
    }

    public Optional<Portfolio> portfolioOf(User user) {
        return find(portfolios, p -> p.getUser().getUserId() == user.getUserId());
    }

    public Optional<Team> teamOf(User user) {
        return find(teams, t -> t.hasMember(user));
    }

    public List<Enrollment> enrollmentsOf(User user) {
        return findAll(enrollments, e -> e.getUser().getUserId() == user.getUserId());
    }

    public Optional<Submission> submissionOf(User user, Assessment assessment) {
        return find(submissions, s -> s.getUser().getUserId() == user.getUserId()
                && s.getAssessment().getAssessmentId() == assessment.getAssessmentId());
    }

    public List<Mentorship> activeMentorshipsOf(Mentor mentor) {
        return findAll(mentorships, m -> m.isActive() && m.getMentor().getUserId() == mentor.getUserId());
    }

    public Optional<Mentorship> activeMentorshipOf(Team team) {
        return find(mentorships, m -> m.isActive() && m.getTeam().getTeamId() == team.getTeamId());
    }

    public AIUsageGuideline getGuideline() {
        return guideline;
    }

    public void setGuideline(AIUsageGuideline guideline) {
        this.guideline = guideline;
    }
}
