package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class Mentor extends User {
    /** A mentor can guide at most three teams at the same time. */
    public static final int MAX_ACTIVE_TEAMS = 3;

    private String expertise;

    public Mentor(int id, String fullName, String email, int age, String country, String expertise) {
        super(id, fullName, email, age, country);
        this.expertise = expertise;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.VIEW_TEAMS, Permission.REVIEW_MILESTONE, Permission.GIVE_FEEDBACK);
    }

    @Override public String getRoleName() { return "Mentor"; }
    @Override public String getRoleLabel() { return "Mentor"; }
    @Override public String getHomeView() { return "mentor"; }

    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
}
