package yep.model;

/** Actions a user may perform. Each User subclass returns its own set (see getPermissions()). */
public enum Permission {
    ENROL, SUBMIT_ASSESSMENT, USE_AI, JOIN_TEAM, SUBMIT_PROJECT, EDIT_PORTFOLIO,
    ADD_CONTENT, MODERATE, GRADE, FORM_TEAMS, GIVE_FEEDBACK,
    VIEW_TEAMS, REVIEW_MILESTONE,
    SEARCH_TALENT, VERIFY_CERTIFICATE,
    VIEW_INSTITUTION_REPORT, VIEW_AGGREGATE_REPORT,
    ASSIGN_MENTOR,
    ALL
}
