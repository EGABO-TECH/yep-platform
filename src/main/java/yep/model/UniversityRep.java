package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class UniversityRep extends OrganizationRepresentative {

    public UniversityRep(int id, String fullName, String email, int age, String country) {
        super(id, fullName, email, age, country);
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.VIEW_INSTITUTION_REPORT, Permission.VERIFY_CERTIFICATE);
    }

    @Override public String getRoleName() { return "UniversityRep"; }
    @Override public String getRoleLabel() { return "University representative"; }
    @Override public String getHomeView() { return "reports"; }
    @Override public String getOrgType() { return "University"; }
}
