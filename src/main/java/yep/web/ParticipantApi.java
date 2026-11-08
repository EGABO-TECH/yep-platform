package yep.web;

import yep.data.DataStore;
import yep.model.*;
import yep.service.AIAssistant;
import yep.service.YepServices;
import yep.util.YepException;

import java.time.LocalDateTime;
import java.util.*;

import static yep.web.Json.obj;

/** Screens used by youth participants: dashboard, learning, assessments, AI assistant, team and portfolio. */
public class ParticipantApi extends ApiModule {

    public ParticipantApi(YepServices services, Views views) {
        super(services, views);
    }

    @Override
    public void register(Router r) {
        r.get("/api/participant/home", this::home);
        r.get("/api/learn", this::learn);
        r.post("/api/enrol", this::enrol);
        r.get("/api/lessons/{id}", this::lesson);
        r.post("/api/lessons/{id}/done", this::lessonDone);
        r.post("/api/lessons/{id}/posts", this::post);
        r.post("/api/posts/{id}/flag", req -> {
            s.learning.flagPost(req.user(), req.pathInt("id"));
            return obj("ok", true, "message", "Thanks. A facilitator will review this comment.");
        });
        r.post("/api/ai/ask", this::ask);
        r.post("/api/assessments/{id}/start", this::startAssessment);
        r.post("/api/assessments/{id}/leave", req -> {
            s.assessments.leaveAssessment(req.user());
            return obj("ok", true);
        });
        r.post("/api/assessments/{id}/submit", this::submitAssessment);
        r.get("/api/team", this::team);
        r.post("/api/teams/{id}/project", this::createProject);
        r.post("/api/milestones/{id}/draft", req -> {
            s.projects.saveDraft(req.user(), req.pathInt("id"), req.str("work"));
            return obj("ok", true, "message", "Draft saved");
        });
        r.post("/api/milestones/{id}/submit", this::submitMilestone);
        r.get("/api/portfolio", this::portfolio);
        r.put("/api/portfolio", this::savePortfolio);
        r.get("/api/portfolio/preview", req -> {
            YouthParticipant p = as(req, YouthParticipant.class);
            Portfolio pf = store.portfolioOf(p).orElseThrow(() -> YepException.notFound("Portfolio"));
            return v.portfolioView(s.portfolios.getPublicView(pf));
        });
    }

    private Programme programme() {
        return store.programmes.get(0);
    }

    private Object home(Request req) {
        YouthParticipant u = as(req, YouthParticipant.class);
        Optional<Enrollment> e = s.learning.enrollmentFor(u, programme());
        Optional<Team> team = store.teamOf(u);
        Optional<Certificate> cert = e.flatMap(en -> DataStore.find(store.certificates,
                c -> c.getEnrollment().getEnrollmentId() == en.getEnrollmentId()));

        Integer continueLesson = null;
        String continueTrack = null;
        for (LearningTrack t : programme().getTracks()) {
            Map<String, Object> summary = v.trackSummary(u, t);
            if (summary.get("nextLessonId") != null) {
                continueLesson = (Integer) summary.get("nextLessonId");
                continueTrack = t.getTitle();
                break;
            }
        }
        Integer nextAssessment = null;
        String nextAssessmentTitle = null;
        if (continueLesson == null) {
            for (Assessment a : programme().getAllAssessments()) {
                if (store.submissionOf(u, a).isEmpty()) {
                    nextAssessment = a.getAssessmentId();
                    nextAssessmentTitle = a.getTitle();
                    break;
                }
            }
        }
        VirtualSession next = store.sessions.stream()
                .filter(vs -> vs.getScheduledAt().isAfter(LocalDateTime.now()))
                .min(Comparator.comparing(VirtualSession::getScheduledAt)).orElse(null);

        String stage = team.isPresent() ? "build" : e.map(Enrollment::isCompleted).orElse(false) ? "eligible" : "learn";
        return obj("name", u.getFullName(), "firstName", u.getFullName().split(" ")[0],
                "enrolled", e.isPresent(),
                "programme", obj("name", programme().getName(), "cohort", e.map(en -> en.getCohort().getName()).orElse(store.cohorts.get(0).getName()),
                        "cohortId", store.cohorts.get(0).getCohortId(),
                        "progress", e.map(Enrollment::getCompletionPct).orElse(0), "completed", e.map(Enrollment::isCompleted).orElse(false),
                        "ends", store.cohorts.get(0).getEndDate().format(Views.DATE)),
                "stage", stage,
                "continueLessonId", continueLesson, "continueTrack", continueTrack,
                "nextAssessmentId", nextAssessment, "nextAssessmentTitle", nextAssessmentTitle,
                "tracks", programme().getTracks().stream().map(t -> v.trackSummary(u, t)).toList(),
                "session", next == null ? null : v.session(next),
                "team", team.map(t -> obj("id", t.getTeamId(), "name", t.getName(), "focusArea", t.getFocusArea(),
                        "project", t.getCurrentProject() == null ? null : t.getCurrentProject().getTitle(),
                        "approved", t.getCurrentProject() == null ? 0 : t.getCurrentProject().countApproved())).orElse(null),
                "certificate", cert.map(v::certificate).orElse(null),
                "notifications", s.notifications.forUser(u).stream().limit(3).map(v::notification).toList());
    }

    private Object learn(Request req) {
        User u = req.user();
        return obj("programme", programme().getName(),
                "enrolled", s.learning.enrollmentFor(u, programme()).isPresent(),
                "cohortId", store.cohorts.get(0).getCohortId(),
                "tracks", programme().getTracks().stream().map(t -> v.trackSummary(u, t)).toList(),
                "sessions", store.sessions.stream().sorted(Comparator.comparing(VirtualSession::getScheduledAt)).map(v::session).toList());
    }

    private Object enrol(Request req) {
        Enrollment e = s.learning.enrol(req.user(), req.intVal("cohortId", store.cohorts.get(0).getCohortId()));
        return obj("ok", true, "message", "You are enrolled in " + e.getCohort().getName());
    }

    private Object lesson(Request req) {
        Lesson l = s.learning.openLesson(req.user(), req.pathInt("id"));
        return v.lesson(req.user(), l);
    }

    private Object lessonDone(Request req) {
        s.learning.markLessonDone(req.user(), req.pathInt("id"));
        Lesson l = s.learning.findLesson(req.pathInt("id"));
        return obj("ok", true, "trackProgress", s.learning.trackProgress(req.user(), l.getTrack()));
    }

    private Object post(Request req) {
        DiscussionPost p = s.learning.postDiscussion(req.user(), req.pathInt("id"), req.str("body"));
        return v.post(p, req.user());
    }

    private Object ask(Request req) {
        Integer lessonId = req.body().get("lessonId") instanceof Number n ? n.intValue() : null;
        AIAssistant.Answer a = s.ai.ask(req.user(), req.str("question"), lessonId);
        return obj("answer", a.text(), "source", a.source(), "sourceLessonId", a.sourceLessonId(),
                "blocked", a.blocked(), "left", a.questionsLeft(), "guideline", store.getGuideline().getVersion());
    }

    private Object startAssessment(Request req) {
        Assessment a = s.assessments.startAssessment(req.user(), req.pathInt("id"));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (Question q : a.getQuestions()) {
            questions.add(obj("text", q.getText(), "options", q.getOptions()));
        }
        return obj("id", a.getAssessmentId(), "title", a.getTitle(), "type", a.getType(), "maxScore", a.getMaxScore(),
                "timeLimit", a.getTimeLimitMinutes(), "prompt", a.getWrittenPrompt(), "questions", questions,
                "track", a.getTrack().getTitle(), "trackId", a.getTrack().getTrackId());
    }

    private Object submitAssessment(Request req) {
        Submission sub = s.assessments.submitAssessment(req.user(), req.pathInt("id"), req.intList("answers"), req.str("writtenAnswer"));
        Assessment a = sub.getAssessment();
        List<Map<String, Object>> review = new ArrayList<>();
        for (int i = 0; i < a.getQuestions().size(); i++) {
            Question q = a.getQuestions().get(i);
            Integer ans = i < sub.getAnswers().size() ? sub.getAnswers().get(i) : null;
            review.add(obj("text", q.getText(), "yours", ans == null ? null : q.getOptions().get(ans),
                    "correctAnswer", q.getOptions().get(q.getCorrectIndex()), "correct", q.isCorrect(ans), "why", q.getExplanation()));
        }
        Enrollment e = s.learning.enrollmentFor(req.user(), a.getTrack().getProgramme()).orElse(null);
        return obj("graded", sub.isGraded(), "score", sub.getScore(), "maxScore", a.getMaxScore(),
                "percentage", Math.round(sub.getPercentage()), "passed", sub.isPassed(), "review", review,
                "programmeProgress", e == null ? 0 : e.getCompletionPct(), "completed", e != null && e.isCompleted(),
                "trackId", a.getTrack().getTrackId());
    }

    private Object team(Request req) {
        YouthParticipant u = as(req, YouthParticipant.class);
        Optional<Team> team = store.teamOf(u);
        if (team.isEmpty()) {
            boolean completed = s.learning.enrollmentFor(u, programme()).map(Enrollment::isCompleted).orElse(false);
            return obj("team", null, "eligible", completed,
                    "progress", s.learning.enrollmentFor(u, programme()).map(Enrollment::getCompletionPct).orElse(0));
        }
        return obj("team", v.team(team.get(), u), "eligible", true);
    }

    private Object createProject(Request req) {
        ImpactProject p = s.projects.createProject(req.user(), req.pathInt("id"), req.str("title"), req.str("description"));
        return v.project(p);
    }

    private Object submitMilestone(Request req) {
        Milestone m = s.projects.submitMilestone(req.user(), req.pathInt("id"), req.str("note"), req.str("link"));
        return obj("ok", true, "message", m.getTitle() + " milestone submitted. Your mentor has been notified.",
                "project", v.project(m.getProject()));
    }

    private Object portfolio(Request req) {
        YouthParticipant u = as(req, YouthParticipant.class);
        return v.portfolio(store.portfolioOf(u).orElseThrow(() -> YepException.notFound("Portfolio")));
    }

    private Object savePortfolio(Request req) {
        Portfolio p = s.portfolios.updatePortfolio(req.user(), req.str("headline"), req.str("bio"),
                req.str("visibility"), req.strList("skills"));
        return obj("ok", true, "message", "Portfolio saved", "portfolio", v.portfolio(p));
    }
}
