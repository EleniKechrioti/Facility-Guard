package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;
import java.util.Objects;

/**
 * Logging of an access event.
 * Every time an AccessCard is used at a Checkpoint,
 * an AccessLog is created.
 */
@Entity
@Table(name = "access_log")
public class AccessLog {

    /** Primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "log_id")
    private int logId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp", nullable = false)
    private Date timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_granted", nullable = false)
    private PermissionType accessGranted;

    /** Entrance or Exit from an Area */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private AccessType accessType;

    /** Card that was used (Many-to-One, owning side) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id", nullable = false)
    private AccessCard accessCard;

    /** CheckpointId (Many-to-One, owning side) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_fk", nullable = false)
    private Checkpoint checkpoint;

    // ================= Constructors =================

    /** default constructor */
    public AccessLog() {
    }

    /**
     * Basic constructor
     */
    public AccessLog(PermissionType accessGranted,
                     AccessType accessType,
                     AccessCard accessCard,
                     Checkpoint checkpoint) {
        this.timestamp = new Date();
        this.accessGranted = accessGranted;
        this.accessType = accessType;
        setAccessCard(accessCard);
        setCheckpoint(checkpoint);
    }

    // ================= Getters / Setters =================

    public int getLogId() {
        return logId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public PermissionType getAccessGranted() {
        return accessGranted;
    }

    public void setAccessGranted(PermissionType accessGranted) {
        this.accessGranted = accessGranted;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public AccessCard getAccessCard() {
        return accessCard;
    }

    /**
     * Helper Method for bidirectional consistency with AccessCard
     */
    public void setAccessCard(AccessCard accessCard) {
        this.accessCard = accessCard;
        if (accessCard != null && !accessCard.getAccessLogs().contains(this)) {
            accessCard.addAccessLog(this);
        }
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

    // ================= equals / hashCode / toString =================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessLog)) return false;
        AccessLog accessLog = (AccessLog) o;

        /** If we have an id, we compare based on the id   */
        if (logId != 0 && accessLog.logId != 0) {
            return logId == accessLog.logId;
        }

        /** Otherwise, "logical" equality – not so critical in tests   */
        return Objects.equals(timestamp, accessLog.timestamp)
                && accessGranted == accessLog.accessGranted
                && accessType == accessLog.accessType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }

    @Override
    public String toString() {
        return "AccessLog{" +
                "logId=" + logId +
                ", timestamp=" + timestamp +
                ", accessGranted=" + accessGranted +
                ", accessType=" + accessType +
                '}';
    }
}
