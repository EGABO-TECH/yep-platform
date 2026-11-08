package yep.service;

import yep.data.DataStore;

/** Creates every service once and wires them together (one place to see how the system fits). */
public class YepServices {
    public final DataStore store;
    public final AuditService audit;
    public final NotificationService notifications;
    public final AuthService auth;
    public final LearningService learning;
    public final CertificateService certificates;
    public final AssessmentService assessments;
    public final AIAssistant ai;
    public final TeamService teams;
    public final ProjectService projects;
    public final MentorshipService mentorship;
    public final PortfolioService portfolios;
    public final ReportService reports;

    public YepServices(DataStore store) {
        this.store = store;
        this.audit = new AuditService(store);
        this.notifications = new NotificationService(store);
        this.auth = new AuthService(store, audit, notifications);
        this.learning = new LearningService(store, auth, notifications, audit);
        this.certificates = new CertificateService(store, notifications, audit);
        this.assessments = new AssessmentService(store, auth, learning, certificates, notifications, audit);
        this.ai = new AIAssistant(store, auth, assessments, audit);
        this.teams = new TeamService(store, auth, notifications, audit);
        this.projects = new ProjectService(store, auth, notifications, audit);
        this.mentorship = new MentorshipService(store, auth, notifications, audit, projects);
        this.portfolios = new PortfolioService(store, auth, audit);
        this.reports = new ReportService(store, auth);
    }
}
