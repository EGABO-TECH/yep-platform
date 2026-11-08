package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Section 7 of the pseudo-code: Portfolio and Employer Access. */
public class PortfolioService {
    private final DataStore store;
    private final AuthService auth;
    private final AuditService audit;

    public PortfolioService(DataStore store, AuthService auth, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.audit = audit;
    }

    /** The view an employer sees: no email, only verified credentials and submitted projects. */
    public record PortfolioView(int portfolioId, String name, String initials, String country, String headline,
                                String bio, List<String> skills, List<Credential> credentials,
                                List<ImpactProject> projects) {
    }

    /** updatePortfolio(participant, headline, bio, visibility). */
    public Portfolio updatePortfolio(User participant, String headline, String bio, String visibility, List<String> skills) {
        auth.checkPermission(participant, Permission.EDIT_PORTFOLIO);
        if (!Portfolio.VISIBILITY_OPTIONS.contains(visibility)) {
            throw new YepException("Invalid visibility option");
        }
        if (headline != null && headline.length() > 90) {
            throw new YepException("Keep your headline under 90 characters");
        }
        Portfolio portfolio = store.portfolioOf(participant).orElseThrow(() -> YepException.notFound("Portfolio"));
        portfolio.setHeadline(headline == null ? "" : headline.trim());
        portfolio.setBio(bio == null ? "" : bio.trim());
        portfolio.setVisibility(visibility);
        if (skills != null) {
            portfolio.setSkills(skills);
        }
        return portfolio;
    }

    /** getPublicView(portfolio). */
    public PortfolioView getPublicView(Portfolio portfolio) {
        YouthParticipant user = portfolio.getUser();
        List<Credential> verified = portfolio.getCredentials().stream().filter(Credential::isVerified).toList();
        List<ImpactProject> projects = store.projects.stream()
                .filter(p -> p.getTeam().hasMember(user)
                        && (!"DRAFT".equals(p.getSubmissionStatus()) || p.countApproved() > 0))
                .toList();
        return new PortfolioView(portfolio.getPortfolioId(), user.getFullName(), user.getInitials(), user.getCountry(),
                portfolio.getHeadline(), portfolio.getBio(), portfolio.getSkills(), verified, projects);
    }

    /** searchTalent(employer, skill, country): most verified credentials first. */
    public List<PortfolioView> searchTalent(User employer, String skill, String country) {
        auth.checkPermission(employer, Permission.SEARCH_TALENT);
        String s = skill == null ? "" : skill.trim();
        String c = country == null ? "" : country.trim();
        List<PortfolioView> results = new ArrayList<>();
        for (Portfolio portfolio : store.portfolios) {
            if (!portfolio.isVisibleToEmployers()) {
                continue;
            }
            boolean skillMatch = s.isEmpty() || portfolio.mentions(s);
            boolean countryMatch = c.isEmpty() || "All".equalsIgnoreCase(c) || portfolio.getUser().getCountry().equalsIgnoreCase(c);
            if (skillMatch && countryMatch && portfolio.getUser().getStatus() == AccountStatus.ACTIVE) {
                results.add(getPublicView(portfolio));
            }
        }
        results.sort(Comparator.comparingInt((PortfolioView v) -> v.credentials().size()).reversed()
                .thenComparing(PortfolioView::name));
        audit.record("TALENT_SEARCH", employer, "skill='" + s + "' country='" + (c.isEmpty() ? "All" : c) + "'");
        return results;
    }

    public PortfolioView viewForEmployer(User employer, int portfolioId) {
        auth.checkPermission(employer, Permission.SEARCH_TALENT);
        Portfolio portfolio = DataStore.find(store.portfolios, p -> p.getPortfolioId() == portfolioId)
                .orElseThrow(() -> YepException.notFound("Portfolio"));
        if (!portfolio.isVisibleToEmployers()) {
            throw new YepException(403, "This portfolio is private");
        }
        audit.record("VIEW_PORTFOLIO", employer, portfolio.getUser().getFullName());
        return getPublicView(portfolio);
    }
}
