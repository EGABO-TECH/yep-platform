package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDate;

/** Pseudo-code: CLASS ProjectService. */
public class ProjectService {
    private final DataStore store;
    private final AuthService auth;
    private final NotificationService notifications;
    private final AuditService audit;

    public ProjectService(DataStore store, AuthService auth, NotificationService notifications, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** createProject(member, teamId, title, description): adds the three standard milestones. */
    public ImpactProject createProject(User member, int teamId, String title, String description) {
        auth.checkPermission(member, Permission.SUBMIT_PROJECT);
        Team team = DataStore.find(store.teams, t -> t.getTeamId() == teamId)
                .orElseThrow(() -> YepException.notFound("Team"));
        if (!team.hasMember(member)) {
            throw new YepException(403, "Only team members can create a project");
        }
        if (team.getCurrentProject() != null) {
            throw new YepException("Your team already has a project");
        }
        if (title == null || title.isBlank()) {
            throw new YepException("Give the project a title");
        }
        ImpactProject project = new ImpactProject(store.newId(), team, title.trim(), description == null ? "" : description.trim());
        LocalDate today = LocalDate.now();
        addMilestone(project, "Planning", today.plusDays(14));
        addMilestone(project, "Prototype", today.plusDays(42));
        addMilestone(project, "Final", today.plusDays(70));
        team.getProjects().add(project);
        store.projects.add(project);
        audit.record("CREATE_PROJECT", member, project.getTitle());
        return project;
    }

    public Milestone addMilestone(ImpactProject project, String title, LocalDate due) {
        Milestone milestone = new Milestone(store.newId(), project, title, due);
        project.getMilestones().add(milestone);
        store.milestones.add(milestone);
        return milestone;
    }

    /** saveWork(member, milestoneId, work): the server copy of a draft (the browser keeps one too). */
    public void saveDraft(User member, int milestoneId, String work) {
        Milestone milestone = findMilestone(milestoneId);
        if (!milestone.getProject().getTeam().hasMember(member)) {
            throw YepException.denied();
        }
        store.milestoneDrafts.put(milestoneId, work == null ? "" : work);
    }

    /** submitMilestone(member, milestoneId): previous milestone must be approved first. */
    public Milestone submitMilestone(User member, int milestoneId, String note, String link) {
        auth.checkPermission(member, Permission.SUBMIT_PROJECT);
        Milestone milestone = findMilestone(milestoneId);
        ImpactProject project = milestone.getProject();
        if (!project.getTeam().hasMember(member)) {
            throw YepException.denied();
        }
        Milestone previous = project.previousOf(milestone);
        if (previous != null && !previous.isApproved()) {
            throw new YepException("Previous milestone must be approved first");
        }
        if (milestone.isApproved()) {
            throw new YepException("This milestone is already approved");
        }
        if ((note == null || note.isBlank()) && (link == null || link.isBlank())) {
            throw new YepException("Describe your work or add a link before submitting");
        }
        milestone.submit(member, note == null ? "" : note.trim(), link == null ? "" : link.trim());
        store.milestoneDrafts.remove(milestoneId);

        if ("Final".equals(milestone.getTitle())) {
            project.submit();
        }
        store.activeMentorshipOf(project.getTeam()).ifPresent(ms ->
                notifications.send(ms.getMentor(), project.getTeam().getName() + ": " + milestone.getTitle()
                        + " milestone is ready for review.", "#/mentor"));
        for (TeamMembership m : project.getTeam().getMembers()) {
            if (m.getUser().getUserId() != member.getUserId()) {
                notifications.send(m.getUser(), member.getFullName() + " submitted the " + milestone.getTitle() + " milestone.", "#/team");
            }
        }
        audit.record("SUBMIT_MILESTONE", member, project.getTitle() + ": " + milestone.getTitle());
        return milestone;
    }

    public Milestone findMilestone(int milestoneId) {
        return DataStore.find(store.milestones, m -> m.getMilestoneId() == milestoneId)
                .orElseThrow(() -> YepException.notFound("Milestone"));
    }

    public ImpactProject findProject(int projectId) {
        return DataStore.find(store.projects, p -> p.getProjectId() == projectId)
                .orElseThrow(() -> YepException.notFound("Project"));
    }
}
