package yep.model;

import java.time.LocalDateTime;

public class TeamMembership {
    private final int membershipId;
    private final Team team;
    private final YouthParticipant user;
    private final String memberRole; // Lead or Member
    private final LocalDateTime joinedAt;

    public TeamMembership(int membershipId, Team team, YouthParticipant user, String memberRole, LocalDateTime joinedAt) {
        this.membershipId = membershipId;
        this.team = team;
        this.user = user;
        this.memberRole = memberRole;
        this.joinedAt = joinedAt;
    }

    public int getMembershipId() { return membershipId; }
    public Team getTeam() { return team; }
    public YouthParticipant getUser() { return user; }
    public String getMemberRole() { return memberRole; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
