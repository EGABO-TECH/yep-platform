package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class Facilitator extends User {
    private String expertiseArea;

    public Facilitator(int id, String fullName, String email, int age, String country, String expertiseArea) {
        super(id, fullName, email, age, country);
        this.expertiseArea = expertiseArea;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.ADD_CONTENT, Permission.MODERATE, Permission.GRADE,
                Permission.FORM_TEAMS, Permission.GIVE_FEEDBACK);
    }

    @Override public String getRoleName() { return "Facilitator"; }
    @Override public String getRoleLabel() { return "Facilitator"; }
    @Override public String getHomeView() { return "facilitator"; }

    public String getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(String expertiseArea) { this.expertiseArea = expertiseArea; }
}
