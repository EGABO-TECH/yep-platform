package yep.service;

import yep.data.DataStore;
import yep.model.*;
import yep.util.PasswordHasher;

import java.time.LocalDate;
import java.util.Optional;

/** Pseudo-code: CertificateService. */
public class CertificateService {
    private final DataStore store;
    private final NotificationService notifications;
    private final AuditService audit;

    public CertificateService(DataStore store, NotificationService notifications, AuditService audit) {
        this.store = store;
        this.notifications = notifications;
        this.audit = audit;
    }

    /** issueCertificate(enrollment). */
    public Certificate issueCertificate(Enrollment enrollment) {
        if (!enrollment.isCompleted()) {
            return null;
        }
        Optional<Certificate> existing = DataStore.find(store.certificates,
                c -> c.getEnrollment().getEnrollmentId() == enrollment.getEnrollmentId());
        if (existing.isPresent()) {
            return existing.get();
        }
        String code;
        do {
            code = "YEP-" + LocalDate.now().getYear() + "-" + PasswordHasher.randomCode(8);
        } while (findByCode(code).isPresent());

        Certificate certificate = new Certificate(store.newId(), enrollment, LocalDate.now(), code);
        store.certificates.add(certificate);

        store.portfolioOf(enrollment.getUser()).ifPresent(portfolio -> portfolio.addCredential(
                new Credential(store.newId(), certificate.getTitle(), "Youth Empowerment Project", true, certificate.getVerificationCode())));

        notifications.send(enrollment.getUser(), "Your certificate for " + certificate.getTitle() + " is ready.", "#/portfolio");
        audit.record("ISSUE_CERTIFICATE", enrollment.getUser(), code);
        return certificate;
    }

    /** verifyCertificate(code). */
    public Optional<Certificate> verifyCertificate(String code) {
        return findByCode(code == null ? "" : code.trim().toUpperCase());
    }

    private Optional<Certificate> findByCode(String code) {
        return DataStore.find(store.certificates, c -> c.getVerificationCode().equals(code));
    }
}
