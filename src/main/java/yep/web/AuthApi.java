package yep.web;

import yep.data.Seeder;
import yep.model.*;
import yep.service.ReportService;
import yep.service.YepServices;
import yep.util.YepException;

import java.util.List;
import java.util.Map;

import static yep.web.Json.obj;

/** Registration, login, logout, notifications and the public certificate check. */
public class AuthApi extends ApiModule {

    public AuthApi(YepServices services, Views views) {
        super(services, views);
    }

    @Override
    public void register(Router r) {
        r.get("/api/meta", this::meta);
        r.post("/api/auth/register", this::registerUser);
        r.post("/api/auth/login", this::login);
        r.post("/api/auth/logout", this::logout);
        r.get("/api/me", this::me);
        r.get("/api/notifications", this::notifications);
        r.post("/api/notifications/read", req -> {
            s.notifications.markAllRead(req.user());
            return obj("ok", true);
        });
        r.get("/api/public/verify", this::verify);
        r.get("/api/public/programme", this::programme);
        r.get("/api/public/certificate/{code}", this::printableCertificate);
    }

    private Object meta(Request req) {
        return obj("countries", ReportService.COUNTRIES,
                "organizations", store.organizations.stream().map(o -> obj("id", o.getOrganizationId(), "name", o.getName(), "type", o.getType())).toList(),
                "roles", List.of(obj("value", "YouthParticipant", "label", "Youth participant (18 to 35)"),
                        obj("value", "Facilitator", "label", "Facilitator"),
                        obj("value", "Mentor", "label", "Mentor"),
                        obj("value", "UniversityRep", "label", "University representative"),
                        obj("value", "Employer", "label", "Employer"),
                        obj("value", "Partner", "label", "Partner organisation")),
                "demoPassword", Seeder.DEMO_PASSWORD,
                "demoCertificate", Seeder.DEMO_CERTIFICATE,
                "demoAccounts", List.of(
                        obj("email", "amina@yep.demo", "name", "Amina Nakato", "role", "Youth participant", "note", "Level One, 40% done"),
                        obj("email", "faith@yep.demo", "name", "Faith Achieng", "role", "Youth participant", "note", "Level Two team lead"),
                        obj("email", "grace@yep.demo", "name", "Grace Atim", "role", "Facilitator", "note", "Grading and teams"),
                        obj("email", "daniel@yep.demo", "name", "Daniel Okello", "role", "Mentor", "note", "1 milestone to review"),
                        obj("email", "allan@yep.demo", "name", "Allan Mugisha", "role", "Employer", "note", "Talent search"),
                        obj("email", "ruth@yep.demo", "name", "Dr. Ruth Nansubuga", "role", "University", "note", "Institution report"),
                        obj("email", "esther@yep.demo", "name", "Esther Wairimu", "role", "Partner", "note", "Outcome report"),
                        obj("email", "admin@yep.demo", "name", "Paul Okot", "role", "Administrator", "note", "2 accounts pending")));
    }

    private Object registerUser(Request req) {
        if (!req.bool("agree")) {
            throw new YepException("Please agree to the privacy policy to continue");
        }
        int age = req.intVal("age", -1);
        if (age < 1 || age > 120) {
            throw new YepException("Enter your age in years");
        }
        Integer orgId = req.body().get("organizationId") instanceof Number n ? n.intValue() : null;
        User user = s.auth.registerUser(req.str("fullName"), req.str("email"), req.str("password"), age,
                req.str("country"), req.str("role"), orgId, req.str("organization"));
        boolean active = user.getStatus() == AccountStatus.ACTIVE;
        if (active && user instanceof YouthParticipant) {
            Cohort cohort = store.cohorts.get(0);
            s.learning.enrol(user, cohort.getCohortId());
        }
        return obj("ok", true, "active", active,
                "message", active ? "Account created. You are enrolled in " + store.cohorts.get(0).getName() + "."
                        : "Account created. An administrator will review it before you can log in.");
    }

    private Object login(Request req) {
        Session session = s.auth.login(req.str("email"), req.str("password"));
        req.exchange().getResponseHeaders().add("Set-Cookie",
                YepServer.COOKIE + "=" + session.getToken() + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=1800");
        return obj("user", v.user(session.getUser()));
    }

    private Object logout(Request req) {
        s.auth.logout(req.token());
        req.exchange().getResponseHeaders().add("Set-Cookie", YepServer.COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        return obj("ok", true, "message", "Logged out");
    }

    private Object me(Request req) {
        User u = req.userOrNull();
        if (u == null) {
            return obj("user", null);
        }
        return obj("user", v.user(u), "unread", s.notifications.unreadCount(u));
    }

    private Object notifications(Request req) {
        User u = req.user();
        return obj("items", s.notifications.forUser(u).stream().limit(30).map(v::notification).toList(),
                "unread", s.notifications.unreadCount(u));
    }

    /** Real programme facts for the landing page: tracks, cohort dates and the next live session. */
    private Object programme(Request req) {
        Programme p = store.programmes.get(0);
        Cohort c = store.cohorts.get(0);
        java.time.format.DateTimeFormatter longDate = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy");
        VirtualSession next = store.sessions.stream()
                .filter(vs -> vs.getScheduledAt().isAfter(java.time.LocalDateTime.now()))
                .min(java.util.Comparator.comparing(VirtualSession::getScheduledAt)).orElse(null);
        return obj("programme", p.getName(),
                "cohort", obj("name", c.getName(), "start", c.getStartDate().format(longDate), "end", c.getEndDate().format(longDate)),
                "firstLessonMinutes", p.getTracks().get(0).getLessons().get(0).getMinutes(),
                "tracks", p.getTracks().stream().map(t -> obj("title", t.getTitle(), "topic", t.getTopic(), "summary", t.getSummary(),
                        "lessons", t.getLessons().size(),
                        "lessonTitles", t.getLessons().stream().map(Lesson::getTitle).toList(),
                        "assessments", t.getAssessments().stream().map(a -> obj("title", a.getTitle(), "type", a.getType(),
                                "minutes", a.getTimeLimitMinutes(), "questions", a.getQuestions().size())).toList())).toList(),
                "session", next == null ? null : obj("title", next.getTitle(), "track", next.getTrack().getTitle(),
                        "when", next.getScheduledAt().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM, HH:mm")) + " EAT",
                        "duration", next.getDurationMins(), "countries", next.getCountries()),
                "milestones", List.of("Planning", "Prototype", "Final"),
                "countries", ReportService.COUNTRIES);
    }

    private Object verify(Request req) {
        String code = req.query("code");
        return s.certificates.verifyCertificate(code)
                .<Object>map(v::certificate)
                .orElse(obj("valid", false, "code", code == null ? "" : code.trim().toUpperCase()));
    }

    private Object printableCertificate(Request req) {
        String code = req.exchange().getRequestURI().getPath().replace("/api/public/certificate/", "");
        Certificate c = s.certificates.verifyCertificate(code).orElseThrow(() -> YepException.notFound("Certificate"));
        Map<String, Object> d = v.certificate(c);
        String html = """
                <!doctype html><html lang="en"><head><meta charset="utf-8"><title>YEP certificate %s</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link rel="stylesheet" href="/css/certificate.css"><link rel="icon" href="/img/favicon.ico" sizes="any"><link rel="icon" href="/img/favicon-32.png" type="image/png"></head>
                <body><main class="cert">
                <img src="/img/yep-logo-light.png" alt="Youth Empowerment Project logo" class="cert-logo">
                <p class="cert-kicker">Certificate of completion</p>
                <h1>%s</h1>
                <p class="cert-lead">has completed the <strong>%s</strong> (%s) of the Youth Empowerment Project,
                covering AI literacy, media literacy, digital safety and intercultural communication.</p>
                <div class="cert-meta"><div><span>Issued</span><b>%s</b></div><div><span>Country</span><b>%s</b></div>
                <div><span>Verification code</span><b>%s</b></div></div>
                <p class="cert-check">Check this certificate at /#/verify?code=%s</p>
                <button onclick="window.print()" class="cert-print">Print or save as PDF</button>
                </main></body></html>
                """.formatted(d.get("code"), esc(d.get("name")), esc(d.get("programme")), esc(d.get("cohort")),
                d.get("issued"), esc(d.get("country")), d.get("code"), d.get("code"));
        return new RawResponse("text/html; charset=utf-8", html, null);
    }

    private static String esc(Object o) {
        return String.valueOf(o).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
