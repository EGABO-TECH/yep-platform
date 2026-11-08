package yep.model;

import java.time.LocalDateTime;

public class AuditLog {
    private final int logId;
    private final String action;
    private final Integer userId;
    private final String userName;
    private final String detail;
    private final LocalDateTime timestamp;

    public AuditLog(int logId, String action, Integer userId, String userName, String detail, LocalDateTime timestamp) {
        this.logId = logId;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.detail = detail;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public String getAction() { return action; }
    public Integer getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getDetail() { return detail; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
