package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Section 5 of the pseudo-code: TeamService. */
public class TeamService {
    public static final int MIN_SELECTED = 4;
    public static final int MAX_TEAM_SIZE = 6;

    private final DataStore store;
    private final AuthService auth;
    private final NotificationService notifications;
    private final AuditService audit;

    public TeamService(DataStore store, AuthService auth, NotificationService notifications, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** listEligible(programmeId): participants who completed Level One and are not yet in a team. */
    public List<YouthParticipant> listEligible(int programmeId) {
        List<YouthParticipant> eligible = new ArrayList<>();
        for (Enrollment e : store.enrollments) {
            if (e.getCohort().getProgramme().getProgrammeId() == programmeId && e.isCompleted()
                    && store.teamOf(e.getUser()).isEmpty()) {
                eligible.add(e.getUser());
            }
        }
        return eligible;
    }

    /** formTeams(facilitator, selected, focusArea): spreads countries across teams, first member is Lead. */
    public List<Team> formTeams(User facilitator, List<Integer> selectedIds, String focusArea) {
        auth.checkPermission(facilitator, Permission.FORM_TEAMS);
        if (selectedIds == null || selectedIds.size() < MIN_SELECTED) {
            throw new YepException("Select at least " + MIN_SELECTED + " participants");
        }
        if (focusArea == null || focusArea.isBlank()) {
            throw new YepException("Choose a focus area for the new teams");
        }
        List<YouthParticipant> selected = new ArrayList<>();
        for (Integer id : selectedIds) {
            User u = store.findUser(id).orElseThrow(() -> YepException.notFound("Participant"));
            if (!(u instanceof YouthParticipant p)) {
                throw new YepException(u.getFullName() + " is not a youth participant");
            }
            if (store.teamOf(p).isPresent()) {
                throw new YepException(p.getFullName() + " is already in a team");
            }
            selected.add(p);
        }

        int numberOfTeams = (int) Math.ceil(selected.size() / (double) MAX_TEAM_SIZE);
        int nextNumber = store.teams.size() + 1;
        List<Team> teams = new ArrayList<>();
        for (int k = 0; k < numberOfTeams; k++) {
            teams.add(new Team(store.newId(), "Team " + (nextNumber + k), focusArea.trim(), LocalDateTime.now()));
        }

        selected.sort(Comparator.comparing(User::getCountry));
        int i = 0;
        for (YouthParticipant participant : selected) {
            Team team = teams.get(i % numberOfTeams);
            String role = team.getMembers().isEmpty() ? "Lead" : "Member";
            team.addMember(new TeamMembership(store.newId(), team, participant, role, LocalDateTime.now()));
            i++;
        }

        for (Team team : teams) {
            store.teams.add(team);
            for (TeamMembership m : team.getMembers()) {
                notifications.send(m.getUser(), "You have been added to " + team.getName() + " (" + team.getFocusArea() + ").", "#/team");
            }
        }
        audit.record("FORM_TEAMS", facilitator, teams.size() + " team(s) in " + focusArea);
        return teams;
    }

    public Team findTeam(int teamId) {
        return DataStore.find(store.teams, t -> t.getTeamId() == teamId)
                .orElseThrow(() -> YepException.notFound("Team"));
    }
}
