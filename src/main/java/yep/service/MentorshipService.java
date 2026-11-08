package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDateTime;

/** Section 6 of the pseudo-code: Mentorship and Feedback. */
public class MentorshipService {
    private final DataStore store;
    private final AuthService auth;
    private final NotificationService notifications;
    private final AuditService audit;
    private final ProjectService projects;

    public MentorshipService(DataStore store, AuthService auth, NotificationService notifications,
                             AuditService audit, ProjectService projects) {
        this.store = store;
        this.auth = auth;
        this.notifications = notifications;
        this.audit = audit;
        this.projects = projects;
    }

    /** assignMentor(admin, mentorId, teamId): a mentor can have at most 3 active teams. */
    public Mentorship assignMentor(User admin, int mentorId, int teamId) {
        auth.checkPermission(admin, Permission.ASSIGN_MENTOR);
        User found = store.findUser(mentorId).orElseThrow(() -> YepException.notFound("Mentor"));
        if (!(found instanceof Mentor mentor)) {
            throw new YepException(found.getFullName() + " is not a mentor");
        }
        Team team = DataStore.find(store.teams, t -> t.getTeamId() == teamId)
                .orElseThrow(() -> YepException.notFound("Team"));
        if (store.activeMentorshipsOf(mentor).size() >= Mentor.MAX_ACTIVE_TEAMS) {
            throw new YepException("Mentor already has " + Mentor.MAX_ACTIVE_TEAMS + " teams");
        }
        store.activeMentorshipOf(team).ifPresent(Mentorship::end);
        Mentorship mentorship = new Mentorship(store.newId(), mentor, team, LocalDateTime.now());
        store.mentorships.add(mentorship);
        notifications.send(mentor, "You have been assigned to " + team.getName() + " (" + team.getFocusArea() + ").", "#/mentor");
        for (TeamMembership m : team.getMembers()) {
            notifications.send(m.getUser(), mentor.getFullName() + " is now your team's mentor.", "#/team");
        }
        audit.record("ASSIGN_MENTOR", admin, mentor.getFullName() + " to " + team.getName());
        return mentorship;
    }

    /** giveFeedback(author, projectId, comment, rating). */
    public Feedback giveFeedback(User author, int projectId, Milestone milestone, String comment, int rating) {
        ImpactProject project = projects.findProject(projectId);
        boolean isMentor = store.mentorships.stream().anyMatch(ms -> ms.isActive()
                && ms.getMentor().getUserId() == author.getUserId()
                && ms.getTeam().getTeamId() == project.getTeam().getTeamId());
        if (!isMentor && !(author instanceof Facilitator)) {
            throw YepException.denied();
        }
        if (rating < 1 || rating > 5) {
            throw new YepException("Rating must be between 1 and 5");
        }
        if (comment == null || comment.isBlank()) {
            throw new YepException("Write a short comment for the team");
        }
        Feedback fb = new Feedback(store.newId(), project, milestone, author, comment.trim(), rating, LocalDateTime.now());
        store.feedback.add(fb);
        for (TeamMembership m : project.getTeam().getMembers()) {
            notifications.send(m.getUser(), "New feedback on " + project.getTitle() + " from " + author.getFullName() + ".", "#/team");
        }
        return fb;
    }

    /** reviewMilestone(mentor, milestoneId, approved, comment, rating). */
    public Milestone reviewMilestone(User mentor, int milestoneId, boolean approved, String comment, int rating) {
        auth.checkPermission(mentor, Permission.REVIEW_MILESTONE);
        Milestone milestone = projects.findMilestone(milestoneId);
        if (!milestone.isSubmitted()) {
            throw new YepException("Milestone has not been submitted");
        }
        giveFeedback(mentor, milestone.getProject().getProjectId(), milestone, comment, rating);
        if (approved) {
            milestone.markApproved();
        } else {
            milestone.markNeedsChanges();
        }
        ImpactProject project = milestone.getProject();
        if (approved && "Final".equals(milestone.getTitle())) {
            completeProject(project);
        }
        audit.record(approved ? "APPROVE_MILESTONE" : "REQUEST_CHANGES", mentor,
                project.getTitle() + ": " + milestone.getTitle());
        return milestone;
    }

    /** When the Final milestone is approved, the project is scored and every member gets a verified credential. */
    private void completeProject(ImpactProject project) {
        double avg = store.feedback.stream()
                .filter(f -> f.getProject().getProjectId() == project.getProjectId())
                .mapToInt(Feedback::getRating).average().orElse(3);
        project.setScore(Math.round(avg * 20));
        project.updateStatus("COMPLETED");
        for (TeamMembership m : project.getTeam().getMembers()) {
            store.portfolioOf(m.getUser()).ifPresent(p -> p.addCredential(new Credential(store.newId(),
                    "Impact project: " + project.getTitle(), "YEP mentor review", true, null)));
            notifications.send(m.getUser(), "Your project " + project.getTitle() + " is complete and added to your portfolio.", "#/portfolio");
        }
    }
}
