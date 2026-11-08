package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class Administrator extends User {
    private int accessLevel;

    public Administrator(int id, String fullName, String email, int age, String country, int accessLevel) {
        super(id, fullName, email, age, country);
        this.accessLevel = accessLevel;
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.ALL);
    }

    @Override public String getRoleName() { return "Administrator"; }
    @Override public String getRoleLabel() { return "Administrator"; }
    @Override public String getHomeView() { return "admin"; }

    public int getAccessLevel() { return accessLevel; }
}
