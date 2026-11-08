package yep.model;

public class Organization {
    private final int organizationId;
    private final String name;
    private final String type; // University, Employer or Partner
    private final String country;

    public Organization(int organizationId, String name, String type, String country) {
        this.organizationId = organizationId;
        this.name = name;
        this.type = type;
        this.country = country;
    }

    public int getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getCountry() { return country; }
}
