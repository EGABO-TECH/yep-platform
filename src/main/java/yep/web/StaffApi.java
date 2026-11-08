package yep.web;

import yep.data.DataStore;
import yep.model.*;
import yep.service.YepServices;
import yep.util.YepException;

import java.time.LocalDateTime;
import java.util.*;

import static yep.web.Json.obj;

/** Facilitator and mentor screens. */
public class StaffApi extends ApiModule {

    public StaffApi(YepServices services, Views views) {
        super(services, views);
    }

    @Override
    public void register(Router r) {
        r.get("/api/facilitator/overview", this::facilitatorOverview);
        r.post("/api/submissions/{id}/grade", this::grade);
        r.post("/api/tracks/{id}/lessons", this::addLesson);
        r.post("/api/posts/{id}/moderate", req -> {
            s.learning.moderatePost(req.user(), req.pathInt("id"), req.bool("hide"));
            return obj("ok", true, "message", req.bool("hide") ? "Comment hidden" : "Comment restored");
        });
        r.post("/api/teams/form", this::formTeams);
        r.get("/api/mentor/overview", this::mentorOverview);
        r.post("/api/milestones/{id}/review", this::review);
    }

    private Object facilitatorOverview(Request req) {
        as(req, Facilitator.class);
        Programme programme = store.programmes.get(0);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        Set<Integer> active = new HashSet<>();
        store.submissions.stream().filter(x -> x.getSubmittedAt().isAfter(weekAgo)).forEach(x -> active.add(x.getUser().getUserId()));
        store.posts.stream().filter(p -> p.getPostedAt().isAfter(weekAgo)).forEach(p -> active.add(p.getAuthor().getUserId()));
        store.aiUsageLogs.stream().filter(l -> l.getAskedAt().isAfter(weekAgo)).forEach(l -> active.add(l.getUser().getUserId()));

        List<Map<String, Object>> toGrade = s.assessments.submissionsToGrade().stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt)).map(v::submissionForGrading).toList();
        List<Map<String, Object>> flagged = store.posts.stream().filter(DiscussionPost::isFlagged)
                .map(p -> v.post(p, null)).toList();
        List<Map<String, Object>> eligible = s.teams.listEligible(programme.getProgrammeId()).stream()
                .map(p -> obj("id", p.getUserId(), "name", p.getFullName(), "initials", p.getInitials(), "country", p.getCountry(),
                        "skills", store.portfolioOf(p).map(Portfolio::getSkills).orElse(List.of()))).toList();

        Map<String, Integer> trackCompletion = new LinkedHashMap<>();
        long enrolled = store.enrollments.size();
        for (LearningTrack t : programme.getTracks()) {
            long done = store.enrollments.stream().filter(en -> s.learning.trackProgress(en.getUser(), t) >= 100).count();
            trackCompletion.put(t.getTitle(), enrolled == 0 ? 0 : (int) Math.round(done * 100.0 / enrolled));
        }
        return obj("stats", obj("active", active.size(), "toGrade", toGrade.size(), "flagged", flagged.size(),
                        "enrolled", enrolled, "completed", store.enrollments.stream().filter(Enrollment::isCompleted).count()),
                "toGrade", toGrade, "flagged", flagged, "eligible", eligible, "trackCompletion", trackCompletion,
                "tracks", programme.getTracks().stream().map(t -> obj("id", t.getTrackId(), "title", t.getTitle(), "lessons", t.getLessons().size())).toList(),
                "teams", store.teams.stream().map(t -> obj("name", t.getName(), "focusArea", t.getFocusArea(), "members", t.getMembers().size(),
                        "mentor", store.activeMentorshipOf(t).map(m -> m.getMentor().getFullName()).orElse(null))).toList(),
                "focusAreas", List.of("Employment", "Education", "Agriculture", "Health", "Climate", "Digital inclusion"));
    }

    private Object grade(Request req) {
        Submission sub = s.assessments.gradeSubmission(req.user(), req.pathInt("id"), req.doubleVal("score"), req.str("feedback"));
        return obj("ok", true, "message", "Grade saved. " + sub.getUser().getFullName() + " has been notified.",
                "passed", sub.isPassed());
    }

    private Object addLesson(Request req) {
        double mb = 0;
        Object raw = req.body().get("videoSizeMb");
        if (raw instanceof Number n) {
            mb = n.doubleValue();
        }
        Lesson l = s.learning.addContent(req.user(), req.pathInt("id"), req.str("title"), req.str("body"), mb);
        return obj("ok", true, "message", "Lesson published in " + l.getTrack().getTitle(), "id", l.getContentId());
    }

    private Object formTeams(Request req) {
        List<Team> teams = s.teams.formTeams(req.user(), req.intList("participantIds"), req.str("focusArea"));
        return obj("ok", true, "message", teams.size() == 1 ? "1 team formed" : teams.size() + " teams formed",
                "teams", teams.stream().map(t -> v.team(t, null)).toList());
    }

    private Object mentorOverview(Request req) {
        Mentor m = as(req, Mentor.class);
        List<Mentorship> active = store.activeMentorshipsOf(m);
        List<Map<String, Object>> teams = new ArrayList<>();
        int toReview = 0;
        for (Mentorship ms : active) {
            Team t = ms.getTeam();
            ImpactProject p = t.getCurrentProject();
            long waiting = p == null ? 0 : p.getMilestones().stream().filter(Milestone::isSubmitted).count();
            toReview += (int) waiting;
            Milestone nextDue = p == null ? null : p.getMilestones().stream().filter(x -> !x.isApproved()).findFirst().orElse(null);
            String badge;
            if (waiting > 0) {
                badge = waiting + " to review";
            } else if (nextDue != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), nextDue.getDueDate());
                badge = days < 0 ? "Overdue" : days <= 14 ? "Due in " + days + " days" : "On track";
            } else {
                badge = p == null ? "No project yet" : "Completed";
            }
            Map<String, Object> view = v.team(t, null);
            view.put("badge", badge);
            view.put("waiting", waiting);
            teams.add(view);
        }
        return obj("name", m.getFullName(), "expertise", m.getExpertise(), "max", Mentor.MAX_ACTIVE_TEAMS,
                "teams", teams, "toReview", toReview);
    }

    private Object review(Request req) {
        Milestone m = s.mentorship.reviewMilestone(req.user(), req.pathInt("id"), req.bool("approved"),
                req.str("comment"), req.intVal("rating", 0));
        return obj("ok", true, "message", (m.isApproved() ? "Milestone approved. " : "Changes requested. ") + "The team has been notified.");
    }
}
