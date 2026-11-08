package yep.model;

import java.time.LocalDate;

public class Certificate {
    private final int certificateId;
    private final Enrollment enrollment;
    private final LocalDate issueDate;
    private final String verificationCode;

    public Certificate(int certificateId, Enrollment enrollment, LocalDate issueDate, String verificationCode) {
        this.certificateId = certificateId;
        this.enrollment = enrollment;
        this.issueDate = issueDate;
        this.verificationCode = verificationCode;
    }

    public String getTitle() {
        return enrollment.getCohort().getProgramme().getName();
    }

    public int getCertificateId() { return certificateId; }
    public Enrollment getEnrollment() { return enrollment; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getVerificationCode() { return verificationCode; }
}
