package yep.model;

/**
 * Class diagram: OrganizationRepresentative. Pseudo-code splits it into
 * EmployerUser, UniversityRep and PartnerUser, so it is abstract here.
 */
public abstract class OrganizationRepresentative extends User {

    protected OrganizationRepresentative(int id, String fullName, String email, int age, String country) {
        super(id, fullName, email, age, country);
    }

    public String getOrganizationName() {
        return getOrganization() == null ? "" : getOrganization().getName();
    }

    public abstract String getOrgType();
}
