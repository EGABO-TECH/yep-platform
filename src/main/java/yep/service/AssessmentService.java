package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.YepException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Section 4 of the pseudo-code: Assessment and Certification. */
public class AssessmentService {
    private final DataStore store;
    private final AuthService auth;
    private final LearningService learning;
    private final CertificateService certificates;
    private final NotificationService notifications;
    private final AuditService audit;
    /** Participants who currently have a graded assessment open (the AI assistant is turned off for them). */
    private final Map<Integer, Integer> openAttempts = new ConcurrentHashMap<>();

    public AssessmentService(DataStore store, AuthService auth, LearningService learning,
                             CertificateService certificates, NotificationService notifications, AuditService audit) {
        this.store = store;
        this.auth = auth;
        this.learning = learning;
        this.certificates = certificates;
        this.notifications = notifications;
        this.audit = audit;
    }

    public Assessment findAssessment(int assessmentId) {
        return DataStore.find(store.assessments, a -> a.getAssessmentId() == assessmentId)
                .orElseThrow(() -> YepException.notFound("Assessment"));
    }

    /** Opens an assessment. From now until it is submitted, the AI assistant is unavailable. */
    public Assessment startAssessment(User user, int assessmentId) {
        auth.checkPermission(user, Permission.SUBMIT_ASSESSMENT);
        Assessment assessment = findAssessment(assessmentId);
        if (learning.enrollmentFor(user, assessment.getTrack().getProgramme()).isEmpty()) {
            throw new YepException("Enrol in this programme first");
        }
        if (store.submissionOf(user, assessment).isPresent()) {
            throw new YepException("Assessment already submitted");
        }
        openAttempts.put(user.getUserId(), assessmentId);
        return assessment;
    }

    public void leaveAssessment(User user) {
        openAttempts.remove(user.getUserId());
    }

    public boolean hasOpenAssessment(User user) {
        return openAttempts.containsKey(user.getUserId());
    }

    /** submitAssessment(participant, assessmentId, answers). */
    public Submission submitAssessment(User user, int assessmentId, List<Integer> answers, String writtenAnswer) {
        auth.checkPermission(user, Permission.SUBMIT_ASSESSMENT);
        Assessment assessment = findAssessment(assessmentId);
        if (store.submissionOf(user, assessment).isPresent()) {
            throw new YepException("Assessment already submitted");
        }
        if (!assessment.isMultipleChoice() && (writtenAnswer == null || writtenAnswer.trim().length() < 20)) {
            throw new YepException("Write at least a few sentences before submitting");
        }
        Submission submission = new Submission(store.newId(), assessment, (YouthParticipant) user,
                answers, writtenAnswer, LocalDateTime.now());
        if (assessment.isMultipleChoice()) {
            submission.setScore(assessment.autoGrade(answers == null ? List.of() : answers));
        }
        store.submissions.add(submission);
        openAttempts.remove(user.getUserId());
        audit.record("SUBMIT_ASSESSMENT", user, assessment.getTitle());

        if (submission.isGraded()) {
            updateProgress((YouthParticipant) user, assessment);
        }
        return submission;
    }

    /** gradeSubmission(facilitator, submissionId, score, feedback). */
    public Submission gradeSubmission(User facilitator, int submissionId, double score, String feedback) {
        auth.checkPermission(facilitator, Permission.GRADE);
        Submission submission = DataStore.find(store.submissions, s -> s.getSubmissionId() == submissionId)
                .orElseThrow(() -> YepException.notFound("Submission"));
        if (score < 0 || score > submission.getAssessment().getMaxScore()) {
            throw new YepException("Invalid score. Enter a number from 0 to " + (int) submission.getAssessment().getMaxScore());
        }
        submission.grade(score, feedback == null ? "" : feedback.trim(), facilitator);
        audit.record("GRADE", facilitator, "Submission " + submissionId + " scored " + score);
        notifications.send(submission.getUser(), "Your " + submission.getAssessment().getTitle() + " has been marked.", "#/learn");
        updateProgress(submission.getUser(), submission.getAssessment());
        return submission;
    }

    /** updateProgress(participant, assessment): completion = passed assessments / all assessments. */
    public void updateProgress(YouthParticipant participant, Assessment assessment) {
        Programme programme = assessment.getTrack().getProgramme();
        Enrollment enrollment = learning.enrollmentFor(participant, programme).orElse(null);
        if (enrollment == null) {
            return;
        }
        int total = 0;
        int passed = 0;
        for (LearningTrack track : programme.getTracks()) {
            for (Assessment a : track.getAssessments()) {
                total++;
                Submission s = store.submissionOf(participant, a).orElse(null);
                if (s != null && s.isGraded() && s.isPassed()) {
                    passed++;
                }
            }
        }
        enrollment.updateProgress(total == 0 ? 0 : (int) Math.round(passed * 100.0 / total));
        if (enrollment.getCompletionPct() == 100 && !enrollment.isCompleted()) {
            enrollment.complete();
            certificates.issueCertificate(enrollment);
        }
    }

    public List<Submission> submissionsToGrade() {
        return DataStore.findAll(store.submissions, s -> !s.isGraded());
    }
}
