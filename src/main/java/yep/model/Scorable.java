package yep.model;

/** Pseudo-code: INTERFACE Scorable. Implemented by Submission and ImpactProject. */
public interface Scorable {
    double getPercentage();

    boolean isPassed();
}
