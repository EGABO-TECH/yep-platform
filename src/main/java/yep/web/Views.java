package yep.web;

import yep.data.DataStore;
import yep.model.*;
import yep.service.PortfolioService;
import yep.service.YepServices;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static yep.web.Json.obj;

/** Turns model objects into the JSON shapes the browser uses. Keeps passwords and private data out. */
public class Views {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm");

    private final YepServices s;
    private final DataStore store;

    public Views(YepServices services) {
        this.s = services;
        this.store = services.store;
    }

    public Map<String, Object> user(User u) {
        return obj("id", u.getUserId(), "name", u.getFullName(), "initials", u.getInitials(), "email", u.getEmail(),
                "role", u.getRoleName(), "roleLabel", u.getRoleLabel(), "country", u.getCountry(), "age", u.getAge(),
                "status", u.getStatus().name(), "organization", u.getOrganization() == null ? null : u.getOrganization().getName(),
                "home", u.getHomeView(), "joined", u.getCreatedAt().format(DATE),
                "permissions", u.getPermissions().stream().map(Enum::name).sorted().toList());
    }

    public Map<String, Object> notification(Notification n) {
        return obj("id", n.getMessageId(), "text", n.getContent(), "link", n.getLink(),
                "when", ago(n.getSentAt()), "read", n.isRead());
    }

    public Map<String, Object> trackSummary(User u, LearningTrack t) {
        int pct = s.learning.trackProgress(u, t);
        Optional<Enrollment> e = s.learning.enrollmentFor(u, t.getProgramme());
        long lessonsDone = e.map(en -> t.getLessons().stream().filter(l -> en.getCompletedLessonIds().contains(l.getContentId())).count()).orElse(0L);
        List<Map<String, Object>> assessments = new ArrayList<>();
        for (Assessment a : t.getAssessments()) {
            assessments.add(assessmentState(u, a));
        }
        Lesson next = e.isEmpty() ? null : t.getLessons().stream()
                .filter(l -> !e.get().getCompletedLessonIds().contains(l.getContentId())).findFirst().orElse(null);
        return obj("id", t.getTrackId(), "title", t.getTitle(), "topic", t.getTopic(), "summary", t.getSummary(),
                "progress", pct, "status", pct >= 100 ? "Completed" : pct == 0 ? "Not started" : pct + "%",
                "lessonsDone", lessonsDone, "lessonCount", t.getLessons().size(),
                "nextLessonId", next == null ? null : next.getContentId(),
                "lessons", t.getLessons().stream().map(l -> obj("id", l.getContentId(), "title", l.getTitle(),
                        "minutes", l.getMinutes(), "hasVideo", l.getVideoTitle() != null,
                        "done", e.map(en -> en.getCompletedLessonIds().contains(l.getContentId())).orElse(false))).toList(),
                "assessments", assessments);
    }

    public Map<String, Object> assessmentState(User u, Assessment a) {
        Optional<Submission> sub = store.submissionOf(u, a);
        String state = sub.isEmpty() ? "NOT_STARTED" : !sub.get().isGraded() ? "AWAITING_MARKING" : sub.get().isPassed() ? "PASSED" : "NOT_PASSED";
        return obj("id", a.getAssessmentId(), "title", a.getTitle(), "type", a.getType(), "maxScore", a.getMaxScore(),
                "questionCount", a.getQuestions().size(), "timeLimit", a.getTimeLimitMinutes(), "state", state,
                "score", sub.map(Submission::getScore).orElse(null),
                "feedback", sub.map(Submission::getFeedback).orElse(null));
    }

    public Map<String, Object> lesson(User u, Lesson l) {
        LearningTrack t = l.getTrack();
        int index = t.getLessons().indexOf(l);
        Optional<Enrollment> e = s.learning.enrollmentFor(u, t.getProgramme());
        Lesson prev = index > 0 ? t.getLessons().get(index - 1) : null;
        Lesson next = index < t.getLessons().size() - 1 ? t.getLessons().get(index + 1) : null;
        return obj("id", l.getContentId(), "title", l.getTitle(), "trackId", t.getTrackId(), "trackTitle", t.getTitle(),
                "position", index + 1, "total", t.getLessons().size(), "paragraphs", l.getParagraphs(),
                "minutes", l.getMinutes(), "videoTitle", l.getVideoTitle(), "videoSizeMb", l.getVideoSizeMb(),
                "done", e.map(en -> en.getCompletedLessonIds().contains(l.getContentId())).orElse(false),
                "prevId", prev == null ? null : prev.getContentId(), "nextId", next == null ? null : next.getContentId(),
                "assessments", t.getAssessments().stream().map(a -> assessmentState(u, a)).toList(),
                "posts", s.learning.postsFor(l.getContentId()).stream().map(p -> post(p, u)).toList());
    }

    public Map<String, Object> post(DiscussionPost p, User viewer) {
        return obj("id", p.getPostId(), "author", p.getAuthor().getFullName(), "initials", p.getAuthor().getInitials(),
                "country", p.getAuthor().getCountry(), "role", p.getAuthor().getRoleLabel(), "body", p.getBody(),
                "when", ago(p.getPostedAt()), "flagged", p.isFlagged(),
                "mine", viewer != null && viewer.getUserId() == p.getAuthor().getUserId(),
                "lessonTitle", p.getLesson().getTitle(), "trackTitle", p.getLesson().getTrack().getTitle());
    }

    public Map<String, Object> submissionForGrading(Submission sub) {
        return obj("id", sub.getSubmissionId(), "participant", sub.getUser().getFullName(), "initials", sub.getUser().getInitials(),
                "country", sub.getUser().getCountry(), "assessment", sub.getAssessment().getTitle(),
                "track", sub.getAssessment().getTrack().getTitle(), "trackId", sub.getAssessment().getTrack().getTrackId(),
                "prompt", sub.getAssessment().getWrittenPrompt(), "answer", sub.getWrittenAnswer(),
                "maxScore", sub.getAssessment().getMaxScore(), "submitted", sub.getSubmittedAt().format(DATE));
    }

    public Map<String, Object> session(VirtualSession v) {
        return obj("id", v.getSessionId(), "title", v.getTitle(), "track", v.getTrack().getTitle(),
                "when", v.getScheduledAt().format(DATE_TIME) + " EAT", "duration", v.getDurationMins(),
                "link", v.getMeetingLink(), "host", v.getHost().getFullName(), "countries", v.getCountries());
    }

    public Map<String, Object> team(Team t, User viewer) {
        ImpactProject p = t.getCurrentProject();
        Mentor mentor = store.activeMentorshipOf(t).map(Mentorship::getMentor).orElse(null);
        return obj("id", t.getTeamId(), "name", t.getName(), "focusArea", t.getFocusArea(),
                "formed", t.getFormedAt().format(DATE),
                "mentor", mentor == null ? null : obj("id", mentor.getUserId(), "name", mentor.getFullName(), "expertise", mentor.getExpertise()),
                "countries", new ArrayList<>(t.getCountries()),
                "members", t.getMembers().stream().map(m -> obj("id", m.getUser().getUserId(), "name", m.getUser().getFullName(),
                        "initials", m.getUser().getInitials(), "country", m.getUser().getCountry(), "role", m.getMemberRole(),
                        "you", viewer != null && viewer.getUserId() == m.getUser().getUserId())).toList(),
                "project", p == null ? null : project(p));
    }

    public Map<String, Object> project(ImpactProject p) {
        List<Feedback> fb = store.feedback.stream().filter(f -> f.getProject().getProjectId() == p.getProjectId())
                .sorted(Comparator.comparing(Feedback::getGivenOn).reversed()).toList();
        List<Map<String, Object>> milestones = new ArrayList<>();
        for (Milestone m : p.getMilestones()) {
            Milestone prev = p.previousOf(m);
            boolean canSubmit = (prev == null || prev.isApproved()) && (Milestone.OPEN.equals(m.getStatus()) || Milestone.NEEDS_CHANGES.equals(m.getStatus()));
            milestones.add(obj("id", m.getMilestoneId(), "title", m.getTitle(), "due", m.getDueDate().format(DATE),
                    "status", m.getStatus(), "canSubmit", canSubmit, "note", m.getWorkNote(), "link", m.getWorkLink(),
                    "submittedBy", m.getSubmittedBy() == null ? null : m.getSubmittedBy().getFullName(),
                    "submittedOn", m.getSubmittedAt() == null ? null : m.getSubmittedAt().format(DATE),
                    "draft", store.milestoneDrafts.get(m.getMilestoneId())));
        }
        return obj("id", p.getProjectId(), "title", p.getTitle(), "description", p.getDescription(),
                "status", p.getSubmissionStatus(), "score", p.getScore(), "approved", p.countApproved(),
                "milestones", milestones,
                "feedback", fb.stream().map(f -> obj("author", f.getAuthor().getFullName(), "comment", f.getComment(),
                        "rating", f.getRating(), "milestone", f.getMilestone() == null ? null : f.getMilestone().getTitle(),
                        "when", f.getGivenOn().format(DATE))).toList());
    }

    public Map<String, Object> portfolio(Portfolio p) {
        List<Map<String, Object>> projects = store.projects.stream().filter(pr -> pr.getTeam().hasMember(p.getUser()))
                .map(pr -> obj("title", pr.getTitle(), "team", pr.getTeam().getName(), "focus", pr.getTeam().getFocusArea(),
                        "status", pr.getSubmissionStatus(), "approved", pr.countApproved())).toList();
        return obj("id", p.getPortfolioId(), "name", p.getUser().getFullName(), "initials", p.getUser().getInitials(),
                "country", p.getUser().getCountry(), "headline", p.getHeadline(), "bio", p.getBio(),
                "visibility", p.getVisibility(), "skills", p.getSkills(),
                "credentials", p.getCredentials().stream().map(this::credential).toList(), "projects", projects);
    }

    public Map<String, Object> credential(Credential c) {
        return obj("id", c.getCredentialId(), "title", c.getTitle(), "issuer", c.getIssuer(), "verified", c.isVerified(), "code", c.getCertificateCode());
    }

    public Map<String, Object> portfolioView(PortfolioService.PortfolioView v) {
        return obj("id", v.portfolioId(), "name", v.name(), "initials", v.initials(), "country", v.country(),
                "headline", v.headline(), "bio", v.bio(), "skills", v.skills(),
                "credentials", v.credentials().stream().map(this::credential).toList(),
                "projects", v.projects().stream().map(p -> obj("title", p.getTitle(), "team", p.getTeam().getName(),
                        "focus", p.getTeam().getFocusArea(), "description", p.getDescription(),
                        "status", p.getSubmissionStatus(), "approved", p.countApproved())).toList());
    }

    public Map<String, Object> certificate(Certificate c) {
        return obj("valid", true, "code", c.getVerificationCode(), "name", c.getEnrollment().getUser().getFullName(),
                "country", c.getEnrollment().getUser().getCountry(), "programme", c.getTitle(),
                "cohort", c.getEnrollment().getCohort().getName(), "issued", c.getIssueDate().format(DATE));
    }

    public Map<String, Object> report(Report r) {
        return obj("title", r.getTitle(), "columns", r.getColumns(), "rows", r.getRows(), "totals", r.getTotals(),
                "generated", r.getGeneratedOn().format(DATE_TIME));
    }

    static String ago(java.time.LocalDateTime t) {
        long mins = java.time.Duration.between(t, java.time.LocalDateTime.now()).toMinutes();
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " min ago";
        long hours = mins / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        if (days < 7) return days + (days == 1 ? " day ago" : " days ago");
        return t.format(DATE);
    }
}
