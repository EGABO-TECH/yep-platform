package yep.service;

import yep.data.DataStore;
import yep.model.AuditLog;
import yep.model.User;

import java.time.LocalDateTime;

/** WRITE AuditLog(...) from the pseudo-code (NFR-13 Audit trail). */
public class AuditService {
    private final DataStore store;

    public AuditService(DataStore store) {
        this.store = store;
    }

    public void record(String action, User user, String detail) {
        store.auditLogs.add(new AuditLog(store.newId(), action,
                user == null ? null : user.getUserId(),
                user == null ? "System" : user.getFullName(),
                detail, LocalDateTime.now()));
    }
}
