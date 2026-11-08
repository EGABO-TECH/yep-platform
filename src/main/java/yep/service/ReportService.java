package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Section 8 of the pseudo-code: Reporting. */
public class ReportService {
    public static final List<String> COUNTRIES = List.of("Uganda", "Kenya", "Tanzania", "Rwanda", "Burundi", "South Sudan", "DR Congo");
    /** Countries with fewer enrolments than this are not shown in detail, to protect privacy. */
    public static final int PRIVACY_THRESHOLD = 5;

    private final DataStore store;
    private final AuthService auth;

    public ReportService(DataStore store, AuthService auth) {
        this.store = store;
        this.auth = auth;
    }

    /** getReport(user, startDate, endDate): picks the right report for the user's role. */
    public Report getReport(User user, String type, LocalDate from, LocalDate to) {
        if (user instanceof Administrator) {
            if ("institution".equals(type)) {
                return institutionReport(null, from, to);
            }
            return "outcome".equals(type) ? outcomeReport(from, to) : systemReport(from, to);
        }
        if (user instanceof UniversityRep && !"outcome".equals(type)) {
            auth.checkPermission(user, Permission.VIEW_INSTITUTION_REPORT);
            return institutionReport(user.getOrganization(), from, to);
        }
        if (user instanceof UniversityRep || user instanceof PartnerUser) {
            // University reps may also see the programme-wide figures.
            if (user instanceof PartnerUser) {
                auth.checkPermission(user, Permission.VIEW_AGGREGATE_REPORT);
            }
            return outcomeReport(from, to);
        }
        throw YepException.denied();
    }

    private boolean inRange(Enrollment e, LocalDate from, LocalDate to) {
        LocalDate start = e.getCohort().getStartDate();
        return (from == null || !start.isBefore(from)) && (to == null || !start.isAfter(to));
    }

    /** institutionReport(organization, startDate, endDate). */
    public Report institutionReport(Organization organization, LocalDate from, LocalDate to) {
        String name = organization == null ? "All institutions" : organization.getName();
        Report report = new Report("Institution Participation Report: " + name,
                List.of("Student", "Country", "Programme", "Completion", "Status"));
        int count = 0;
        int completed = 0;
        for (User student : store.users) {
            if (!(student instanceof YouthParticipant)) {
                continue;
            }
            if (organization != null && (student.getOrganization() == null
                    || student.getOrganization().getOrganizationId() != organization.getOrganizationId())) {
                continue;
            }
            for (Enrollment e : store.enrollmentsOf(student)) {
                if (inRange(e, from, to)) {
                    report.addRow(student.getFullName(), student.getCountry(), e.getCohort().getProgramme().getName(),
                            e.getCompletionPct() + "%", e.isCompleted() ? "Completed" : "In progress");
                    count++;
                    if (e.isCompleted()) {
                        completed++;
                    }
                }
            }
        }
        report.addTotal("Students enrolled", count);
        report.addTotal("Completed", completed);
        report.addTotal("Completion rate", count == 0 ? "0%" : Math.round(completed * 100.0 / count) + "%");
        return report;
    }

    /** outcomeReport(startDate, endDate): completion by country, with small numbers hidden. */
    public Report outcomeReport(LocalDate from, LocalDate to) {
        Report report = new Report("Programme Outcome Report", List.of("Country", "Enrolled", "Completed"));
        Map<String, int[]> byCountry = new TreeMap<>();
        for (Enrollment e : store.enrollments) {
            if (inRange(e, from, to)) {
                int[] counts = byCountry.computeIfAbsent(e.getUser().getCountry(), k -> new int[2]);
                counts[0]++;
                if (e.isCompleted()) {
                    counts[1]++;
                }
            }
        }
        for (String country : COUNTRIES) {
            int[] counts = byCountry.get(country);
            if (counts == null) {
                continue;
            }
            if (counts[0] < PRIVACY_THRESHOLD) {
                report.addRow(country, "Less than " + PRIVACY_THRESHOLD, "-");
            } else {
                report.addRow(country, String.valueOf(counts[0]), Math.round(counts[1] * 100.0 / counts[0]) + "%");
            }
        }
        report.addTotal("Certificates issued", store.certificates.size());
        report.addTotal("Teams formed", store.teams.size());
        report.addTotal("Projects submitted", store.projects.stream().filter(p -> !"DRAFT".equals(p.getSubmissionStatus())).count());
        return report;
    }

    /** systemReport(startDate, endDate). */
    public Report systemReport(LocalDate from, LocalDate to) {
        Report report = new Report("System Usage Report", List.of("Measure", "Value"));
        long newUsers = store.users.stream().filter(u -> {
            LocalDate d = u.getCreatedAt().toLocalDate();
            return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
        }).count();
        report.addRow("New users", String.valueOf(newUsers));
        report.addRow("Active users", String.valueOf(store.users.stream().filter(u -> u.getStatus() == AccountStatus.ACTIVE).count()));
        report.addRow("Pending approval", String.valueOf(store.users.stream().filter(u -> u.getStatus() == AccountStatus.PENDING).count()));
        report.addRow("Locked accounts", String.valueOf(store.users.stream().filter(u -> u.getStatus() == AccountStatus.LOCKED).count()));
        report.addRow("AI questions asked", String.valueOf(store.aiUsageLogs.size()));
        report.addRow("Discussion posts", String.valueOf(store.posts.size()));
        report.addTotal("Certificates issued", store.certificates.size());
        report.addTotal("Teams formed", store.teams.size());
        report.addTotal("Projects submitted", store.projects.stream().filter(p -> !"DRAFT".equals(p.getSubmissionStatus())).count());
        return report;
    }
}
