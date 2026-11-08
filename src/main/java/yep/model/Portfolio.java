package yep.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Portfolio {
    public static final List<String> VISIBILITY_OPTIONS = List.of("PRIVATE", "EMPLOYERS", "PUBLIC");

    private final int portfolioId;
    private final YouthParticipant user;
    private String headline = "";
    private String bio = "";
    private String visibility = "PRIVATE";
    private final List<String> skills = new ArrayList<>();
    private final List<Credential> credentials = new ArrayList<>();

    public Portfolio(int portfolioId, YouthParticipant user) {
        this.portfolioId = portfolioId;
        this.user = user;
    }

    /** addCredential(credential) from the pseudo-code. */
    public void addCredential(Credential credential) {
        credentials.add(credential);
    }

    public long countVerified() {
        return credentials.stream().filter(Credential::isVerified).count();
    }

    public boolean isVisibleToEmployers() {
        return "EMPLOYERS".equals(visibility) || "PUBLIC".equals(visibility);
    }

    /** "portfolio MENTIONS skill" from searchTalent(). */
    public boolean mentions(String skill) {
        String needle = skill.toLowerCase(Locale.ROOT).trim();
        if (headline.toLowerCase(Locale.ROOT).contains(needle) || bio.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        for (String s : skills) {
            if (s.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        for (Credential c : credentials) {
            if (c.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public void setSkills(List<String> newSkills) {
        skills.clear();
        for (String s : newSkills) {
            if (s != null && !s.isBlank() && skills.size() < 12) {
                skills.add(s.trim());
            }
        }
    }

    public int getPortfolioId() { return portfolioId; }
    public YouthParticipant getUser() { return user; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public List<String> getSkills() { return skills; }
    public List<Credential> getCredentials() { return credentials; }
}
