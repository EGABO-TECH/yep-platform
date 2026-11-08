package yep.web;

import yep.model.*;
import yep.service.YepServices;
import yep.util.YepException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static yep.web.Json.obj;

/** Employer talent search and the reports used by universities, partners and administrators. */
public class OrgApi extends ApiModule {

    public OrgApi(YepServices services, Views views) {
        super(services, views);
    }

    @Override
    public void register(Router r) {
        r.get("/api/talent", req -> {
            var results = s.portfolios.searchTalent(req.user(), req.query("skill"), req.query("country"));
            return obj("results", results.stream().map(v::portfolioView).toList(), "count", results.size());
        });
        r.get("/api/talent/{id}", req -> v.portfolioView(s.portfolios.viewForEmployer(req.user(), req.pathInt("id"))));
        r.get("/api/reports", this::report);
        r.get("/api/reports/export", this::export);
    }

    private LocalDate date(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new YepException("Use a valid date");
        }
    }

    private Report build(Request req) {
        LocalDate from = date(req.query("from"));
        LocalDate to = date(req.query("to"));
        if (from != null && to != null && from.isAfter(to)) {
            throw new YepException("The start date must be before the end date");
        }
        return s.reports.getReport(req.user(), req.query("type"), from, to);
    }

    private Object report(Request req) {
        User u = req.user();
        Report report = build(req);
        Map<String, Object> out = v.report(report);
        out.put("canInstitution", u instanceof UniversityRep || u instanceof Administrator);
        out.put("canOutcome", true);
        out.put("organization", u.getOrganization() == null ? null : u.getOrganization().getName());
        return out;
    }

    private Object export(Request req) {
        Report report = build(req);
        s.audit.record("EXPORT_REPORT", req.user(), report.getTitle());
        String name = report.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".csv";
        return new RawResponse("text/csv; charset=utf-8", report.toCsv(), name);
    }
}
