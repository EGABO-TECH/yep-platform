package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class YouthParticipant extends User {
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 35;

    private String educationLevel = "";
    private String digitalSkillLevel = "Beginner";

    public YouthParticipant(int id, String fullName, String email, int age, String country) {
        super(id, fullName, email, age, country);
    }

    public static boolean isEligibleAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.ENROL, Permission.SUBMIT_ASSESSMENT, Permission.USE_AI,
                Permission.JOIN_TEAM, Permission.SUBMIT_PROJECT, Permission.EDIT_PORTFOLIO);
    }

    @Override public String getRoleName() { return "YouthParticipant"; }
    @Override public String getRoleLabel() { return "Youth participant"; }
    @Override public String getHomeView() { return "home"; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getDigitalSkillLevel() { return digitalSkillLevel; }
    public void setDigitalSkillLevel(String digitalSkillLevel) { this.digitalSkillLevel = digitalSkillLevel; }
}
