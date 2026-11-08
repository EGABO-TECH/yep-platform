package yep.web;

import yep.model.*;
import yep.service.YepServices;
import yep.util.YepException;

import java.time.LocalDate;
import java.util.*;

import static yep.web.Json.obj;

/** Administrator screens: approvals, roles, locked accounts, mentor assignment, AI rules and the audit log. */
public class AdminApi extends ApiModule {

    public AdminApi(YepServices services, Views views) {
        super(services, views);
    }

    @Override
    public void register(Router r) {
        r.get("/api/admin/overview", this::overview);
        r.get("/api/admin/users", this::users);
        r.post("/api/admin/users/{id}/approve", req -> done(s.auth.approveUser(req.user(), req.pathInt("id")), "approved"));
        r.post("/api/admin/users/{id}/reject", req -> done(s.auth.rejectUser(req.user(), req.pathInt("id")), "rejected"));
        r.post("/api/admin/users/{id}/unlock", req -> done(s.auth.unlockUser(req.user(), req.pathInt("id")), "unlocked"));
        r.post("/api/admin/users/{id}/role", req -> {
            User u = s.auth.changeRole(req.user(), req.pathInt("id"), req.str("role"));
            return obj("ok", true, "message", u.getFullName() + " is now registered as " + u.getRoleLabel().toLowerCase(), "user", v.user(u));
        });
        r.get("/api/admin/mentoring", this::mentoring);
        r.post("/api/admin/mentorships", req -> {
            Mentorship m = s.mentorship.assignMentor(req.user(), req.intVal("mentorId", -1), req.intVal("teamId", -1));
            return obj("ok", true, "message", m.getMentor().getFullName() + " now mentors " + m.getTeam().getName());
        });
        r.get("/api/admin/ai-guidelines", req -> {
            as(req, Administrator.class);
            return guideline();
        });
        r.put("/api/admin/ai-guidelines", this::saveGuideline);
        r.get("/api/admin/audit", this::audit);
    }

    private Object done(User u, String verb) {
        return obj("ok", true, "message", u.getFullName() + " " + verb, "user", v.user(u));
    }

    private Object overview(Request req) {
        as(req, Administrator.class);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (AccountStatus st : AccountStatus.values()) {
            counts.put(st.name(), store.users.stream().filter(u -> u.getStatus() == st).count());
        }
        return obj("counts", counts, "report", v.report(s.reports.systemReport(null, null)));
    }

    private Object users(Request req) {
        as(req, Administrator.class);
        String status = Optional.ofNullable(req.query("status")).orElse("PENDING");
        String q = Optional.ofNullable(req.query("q")).orElse("").toLowerCase().trim();
        List<Map<String, Object>> list = store.users.stream()
                .filter(u -> "ALL".equals(status) || u.getStatus().name().equals(status))
                .filter(u -> q.isEmpty() || u.getFullName().toLowerCase().contains(q) || u.getEmail().contains(q))
                .sorted(Comparator.comparing(User::getFullName))
                .map(v::user).toList();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PENDING", store.users.stream().filter(u -> u.getStatus() == AccountStatus.PENDING).count());
        counts.put("ALL", (long) store.users.size());
        counts.put("LOCKED", store.users.stream().filter(u -> u.getStatus() == AccountStatus.LOCKED).count());
        return obj("users", list, "counts", counts,
                "roles", List.of(obj("value", "Facilitator", "label", "Facilitator"), obj("value", "Mentor", "label", "Mentor"),
                        obj("value", "UniversityRep", "label", "University representative"), obj("value", "Employer", "label", "Employer"),
                        obj("value", "Partner", "label", "Partner organisation")));
    }

    private Object mentoring(Request req) {
        as(req, Administrator.class);
        List<Map<String, Object>> mentors = store.users.stream().filter(u -> u instanceof Mentor && u.getStatus() == AccountStatus.ACTIVE)
                .map(u -> obj("id", u.getUserId(), "name", u.getFullName(), "expertise", ((Mentor) u).getExpertise(),
                        "active", store.activeMentorshipsOf((Mentor) u).size(), "max", Mentor.MAX_ACTIVE_TEAMS)).toList();
        List<Map<String, Object>> teams = store.teams.stream().map(t -> obj("id", t.getTeamId(), "name", t.getName(),
                "focusArea", t.getFocusArea(), "members", t.getMembers().size(),
                "mentor", store.activeMentorshipOf(t).map(m -> m.getMentor().getFullName()).orElse(null))).toList();
        return obj("mentors", mentors, "teams", teams);
    }

    private Map<String, Object> guideline() {
        AIUsageGuideline g = store.getGuideline();
        return obj("version", g.getVersion(), "rules", g.getRules(), "dailyLimit", g.getDailyLimit(),
                "effectiveFrom", g.getEffectiveFrom().format(Views.DATE),
                "questionsToday", store.aiUsageLogs.stream().filter(l -> l.getAskedAt().toLocalDate().equals(LocalDate.now())).count());
    }

    private Object saveGuideline(Request req) {
        Administrator admin = as(req, Administrator.class);
        List<String> rules = req.strList("rules").stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
        int limit = req.intVal("dailyLimit", 30);
        if (rules.isEmpty()) {
            throw new YepException("Add at least one rule");
        }
        if (limit < 1 || limit > 200) {
            throw new YepException("Daily limit must be between 1 and 200");
        }
        String[] parts = store.getGuideline().getVersion().split("\\.");
        String version = parts[0] + "." + (Integer.parseInt(parts[1]) + 1);
        store.setGuideline(new AIUsageGuideline(version, rules, limit, LocalDate.now()));
        s.audit.record("AI_GUIDELINES", admin, "Published version " + version);
        Map<String, Object> out = guideline();
        out.put("message", "AI guidelines version " + version + " published");
        return out;
    }

    private Object audit(Request req) {
        as(req, Administrator.class);
        List<AuditLog> logs = new ArrayList<>(store.auditLogs);
        Collections.reverse(logs);
        return obj("items", logs.stream().limit(200).map(l -> obj("action", l.getAction(), "user", l.getUserName(),
                "detail", l.getDetail(), "when", l.getTimestamp().format(Views.DATE_TIME))).toList());
    }
}
