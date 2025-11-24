package org.aueb.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents a security alert caused by an unauthorized or abnormal access attempt.
 */
@Entity
@Table(name = "alerts")
public class Alert implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "alert_id")
    private int alertId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date timestamp;

    @Column(nullable = false, length = 500)
    private String message;

    /**
     * Each Alert is linked to exactly one AccessLog event that triggered it.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private AccessLog accessLog;

    public Alert() {}

    public Alert(Date timestamp, String message) {
        this.timestamp = timestamp;
        this.message = message;
    }

    public int getAlertId() {
        return alertId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public AccessLog getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(AccessLog accessLog) {
        this.accessLog = accessLog;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "alertId=" + alertId +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
}
