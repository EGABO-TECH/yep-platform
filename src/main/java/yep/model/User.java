package yep.model;

import yep.util.PasswordHasher;
import yep.util.YepException;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Abstract base class for every person who uses YEP (class diagram: «abstract» User).
 * Subclasses decide their own permissions and home screen (polymorphism).
 */
public abstract class User {
    private final int userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private int age;
    private String country;
    protected AccountStatus status;
    private int failedLogins;
    private Organization organization;
    private final LocalDateTime createdAt;

    protected User(int userId, String fullName, String email, int age, String country) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email.toLowerCase().trim();
        this.age = age;
        this.country = country;
        this.status = AccountStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /** setPassword(plainPassword) from the pseudo-code. */
    public void setPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.length() < 8) {
            throw new YepException("Password must have at least 8 characters");
        }
        this.passwordHash = PasswordHasher.hash(plainPassword);
    }

    /** passwordMatches(plainPassword) from the pseudo-code. */
    public boolean passwordMatches(String plainPassword) {
        return PasswordHasher.verify(plainPassword, passwordHash);
    }

    /** Used when an administrator changes a pending account's role: the new object keeps the old password. */
    public void copyPasswordFrom(User other) {
        this.passwordHash = other.passwordHash;
    }

    public boolean hasPermission(Permission action) {
        Set<Permission> permissions = getPermissions();
        return status == AccountStatus.ACTIVE
                && (permissions.contains(action) || permissions.contains(Permission.ALL));
    }

    public void recordFailedLogin() {
        failedLogins++;
        if (failedLogins >= 5) {
            status = AccountStatus.LOCKED;
        }
    }

    public void resetFailedLogins() {
        failedLogins = 0;
    }

    public void updateProfile(String fullName, String country) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName.trim();
        }
        if (country != null && !country.isBlank()) {
            this.country = country;
        }
    }

    public String getInitials() {
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    /** Each role has its own set of allowed actions. */
    public abstract Set<Permission> getPermissions();

    /** Internal role name, for example "YouthParticipant". */
    public abstract String getRoleName();

    /** Role name shown to people, for example "Youth participant". */
    public abstract String getRoleLabel();

    /** The screen this user sees after logging in (getDashboard() in the pseudo-code). */
    public abstract String getHomeView();

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    public String getCountry() { return country; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public int getFailedLogins() { return failedLogins; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
