package yep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Team {
    private final int teamId;
    private final String name;
    private final String focusArea;
    private final LocalDateTime formedAt;
    private final List<TeamMembership> members = new ArrayList<>();
    private final List<ImpactProject> projects = new ArrayList<>();

    public Team(int teamId, String name, String focusArea, LocalDateTime formedAt) {
        this.teamId = teamId;
        this.name = name;
        this.focusArea = focusArea;
        this.formedAt = formedAt;
    }

    /** hasMember(user) from the pseudo-code. */
    public boolean hasMember(User user) {
        for (TeamMembership m : members) {
            if (m.getUser().getUserId() == user.getUserId()) {
                return true;
            }
        }
        return false;
    }

    public void addMember(TeamMembership membership) {
        members.add(membership);
    }

    public void removeMember(User user) {
        members.removeIf(m -> m.getUser().getUserId() == user.getUserId());
    }

    public TeamMembership getLead() {
        return members.stream().filter(m -> "Lead".equals(m.getMemberRole())).findFirst().orElse(null);
    }

    public Set<String> getCountries() {
        Set<String> countries = new LinkedHashSet<>();
        members.forEach(m -> countries.add(m.getUser().getCountry()));
        return countries;
    }

    public ImpactProject getCurrentProject() {
        return projects.isEmpty() ? null : projects.get(projects.size() - 1);
    }

    public int getTeamId() { return teamId; }
    public String getName() { return name; }
    public String getFocusArea() { return focusArea; }
    public LocalDateTime getFormedAt() { return formedAt; }
    public List<TeamMembership> getMembers() { return members; }
    public List<ImpactProject> getProjects() { return projects; }
}
