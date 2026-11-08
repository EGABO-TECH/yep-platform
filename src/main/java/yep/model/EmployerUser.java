package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class EmployerUser extends OrganizationRepresentative {

    public EmployerUser(int id, String fullName, String email, int age, String country) {
        super(id, fullName, email, age, country);
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.SEARCH_TALENT, Permission.VERIFY_CERTIFICATE);
    }

    @Override public String getRoleName() { return "Employer"; }
    @Override public String getRoleLabel() { return "Employer"; }
    @Override public String getHomeView() { return "talent"; }
    @Override public String getOrgType() { return "Employer"; }
}
