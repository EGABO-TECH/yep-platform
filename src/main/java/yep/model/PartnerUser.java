package yep.model;

import java.util.EnumSet;
import java.util.Set;

public class PartnerUser extends OrganizationRepresentative {

    public PartnerUser(int id, String fullName, String email, int age, String country) {
        super(id, fullName, email, age, country);
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.VIEW_AGGREGATE_REPORT, Permission.VERIFY_CERTIFICATE);
    }

    @Override public String getRoleName() { return "Partner"; }
    @Override public String getRoleLabel() { return "Partner organisation"; }
    @Override public String getHomeView() { return "reports"; }
    @Override public String getOrgType() { return "Partner"; }
}
