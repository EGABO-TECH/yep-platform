package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.PasswordHasher;
import yep.util.YepException;

import java.util.Optional;

/** Section 2 of the pseudo-code: Account Management. */
public class AuthService {
    private final DataStore store;
    private final AuditService audit;
    private final NotificationService notifications;

    public AuthService(DataStore store, AuditService audit, NotificationService notifications) {
        this.store = store;
        this.audit = audit;
        this.notifications = notifications;
    }

    /** registerUser(fullName, email, password, age, country, roleName, organizationId). */
    public User registerUser(String fullName, String email, String password, int age, String country,
                             String roleName, Integer organizationId, String organizationName) {
        if (isBlank(fullName) || isBlank(email) || isBlank(password)) {
            throw new YepException("Please fill in all required fields");
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new YepException("Enter a valid email address");
        }
        if (store.findUserByEmail(email).isPresent()) {
            throw new YepException("Email already registered");
        }
        if ("YouthParticipant".equals(roleName) && !YouthParticipant.isEligibleAge(age)) {
            throw new YepException("Only youth aged 18 to 35 can register");
        }

        User user = createUserOfRole(roleName, fullName.trim(), email, age, country);
        user.setPassword(password);
        user.setOrganization(resolveOrganization(organizationId, organizationName, user));

        if (user instanceof YouthParticipant participant) {
            user.setStatus(AccountStatus.ACTIVE);
            store.portfolios.add(new Portfolio(store.newId(), participant));
        } else {
            user.setStatus(AccountStatus.PENDING);
        }
        store.users.add(user);
        audit.record("REGISTER", user, user.getRoleLabel() + " account created");
        return user;
    }

    /** CREATE new object of class roleName: a small factory that returns the right subclass. */
    public User createUserOfRole(String roleName, String fullName, String email, int age, String country) {
        int id = store.newId();
        return switch (roleName == null ? "" : roleName) {
            case "YouthParticipant" -> new YouthParticipant(id, fullName, email, age, country);
            case "Facilitator" -> new Facilitator(id, fullName, email, age, country, "");
            case "Mentor" -> new Mentor(id, fullName, email, age, country, "");
            case "Employer" -> new EmployerUser(id, fullName, email, age, country);
            case "UniversityRep" -> new UniversityRep(id, fullName, email, age, country);
            case "Partner" -> new PartnerUser(id, fullName, email, age, country);
            default -> throw new YepException("Choose a valid account type");
        };
    }

    private Organization resolveOrganization(Integer organizationId, String organizationName, User user) {
        if (organizationId != null) {
            Optional<Organization> org = DataStore.find(store.organizations, o -> o.getOrganizationId() == organizationId);
            if (org.isPresent()) {
                return org.get();
            }
        }
        if (!isBlank(organizationName)) {
            String name = organizationName.trim();
            Optional<Organization> existing = DataStore.find(store.organizations, o -> o.getName().equalsIgnoreCase(name));
            if (existing.isPresent()) {
                return existing.get();
            }
            String type = user instanceof OrganizationRepresentative rep ? rep.getOrgType() : "University";
            Organization created = new Organization(store.newId(), name, type, user.getCountry());
            store.organizations.add(created);
            return created;
        }
        return null;
    }

    /** login(email, password): returns a session or throws with the message to display. */
    public Session login(String email, String password) {
        User user = store.findUserByEmail(email).orElse(null);
        if (user == null) {
            throw new YepException(401, "Wrong email or password");
        }
        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new YepException(423, "Account locked. Contact the administrator");
        }
        if (!user.passwordMatches(password == null ? "" : password)) {
            user.recordFailedLogin();
            if (user.getStatus() == AccountStatus.LOCKED) {
                audit.record("ACCOUNT_LOCKED", user, "Locked after 5 wrong passwords");
                throw new YepException(423, "Account locked after 5 wrong attempts. Contact the administrator");
            }
            int left = 5 - user.getFailedLogins();
            throw new YepException(401, "Wrong email or password. " + left + (left == 1 ? " attempt" : " attempts") + " left");
        }
        if (user.getStatus() == AccountStatus.PENDING) {
            throw new YepException(403, "Account awaiting approval by the administrator");
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new YepException(403, "This account application was not approved");
        }
        user.resetFailedLogins();
        Session session = new Session(PasswordHasher.randomToken(), user);
        store.loginSessions.put(session.getToken(), session);
        audit.record("LOGIN", user, "Signed in");
        return session;
    }

    /** Returns the user for a session token, or null if the session is missing or expired. */
    public User currentUser(String token) {
        if (token == null) {
            return null;
        }
        Session session = store.loginSessions.get(token);
        if (session == null) {
            return null;
        }
        if (session.isExpired() || session.getUser().getStatus() != AccountStatus.ACTIVE) {
            store.loginSessions.remove(token);
            return null;
        }
        session.touch();
        return session.getUser();
    }

    /** checkPermission(user, action). Throws "Access denied" and writes an audit entry. */
    public void checkPermission(User user, Permission action) {
        if (user == null) {
            throw new YepException(401, "Please log in");
        }
        if (!user.hasPermission(action)) {
            audit.record("ACCESS_DENIED", user, action.name());
            throw YepException.denied();
        }
    }

    /** approveUser(admin, userId). */
    public User approveUser(User admin, int userId) {
        checkPermission(admin, Permission.ALL);
        User user = store.findUser(userId).orElseThrow(() -> YepException.notFound("User"));
        user.setStatus(AccountStatus.ACTIVE);
        user.resetFailedLogins();
        notifications.send(user, "Your account has been approved. Welcome to YEP.", "#/home");
        audit.record("APPROVE_USER", admin, user.getFullName() + " (" + user.getRoleLabel() + ")");
        return user;
    }

    public User rejectUser(User admin, int userId) {
        checkPermission(admin, Permission.ALL);
        User user = store.findUser(userId).orElseThrow(() -> YepException.notFound("User"));
        user.setStatus(AccountStatus.REJECTED);
        audit.record("REJECT_USER", admin, user.getFullName());
        return user;
    }

    public User unlockUser(User admin, int userId) {
        checkPermission(admin, Permission.ALL);
        User user = store.findUser(userId).orElseThrow(() -> YepException.notFound("User"));
        user.resetFailedLogins();
        user.setStatus(AccountStatus.ACTIVE);
        notifications.send(user, "Your account has been unlocked. You can log in again.", "#/home");
        audit.record("UNLOCK_USER", admin, user.getFullName());
        return user;
    }

    /** assignRole(): replaces a pending account with an object of the new class, keeping its details. */
    public User changeRole(User admin, int userId, String newRole) {
        checkPermission(admin, Permission.ALL);
        User old = store.findUser(userId).orElseThrow(() -> YepException.notFound("User"));
        if (old instanceof YouthParticipant || old instanceof Administrator) {
            throw new YepException("Only pending staff and organisation accounts can change role");
        }
        if (old.getRoleName().equals(newRole)) {
            return old;
        }
        User replacement = createUserOfRole(newRole, old.getFullName(), old.getEmail(), old.getAge(), old.getCountry());
        copyPassword(old, replacement);
        replacement.setOrganization(old.getOrganization());
        replacement.setStatus(old.getStatus());
        store.users.remove(old);
        store.users.add(replacement);
        audit.record("CHANGE_ROLE", admin, old.getFullName() + ": " + old.getRoleLabel() + " to " + replacement.getRoleLabel());
        return replacement;
    }

    private void copyPassword(User from, User to) {
        to.copyPasswordFrom(from);
    }

    /** logout(session). */
    public void logout(String token) {
        Session session = token == null ? null : store.loginSessions.remove(token);
        if (session != null) {
            audit.record("LOGOUT", session.getUser(), "Signed out");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
