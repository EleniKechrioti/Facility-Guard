package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.PermissionType;

import java.util.Date;
import java.util.Objects;

/**
 * Καταγραφή ενός συμβάντος πρόσβασης.
 * Κάθε φορά που μία AccessCard χρησιμοποιείται σε ένα Checkpoint,
 * δημιουργείται ένα AccessLog.
 */
@Entity
@Table(name = "access_log")
public class AccessLog {

    /** Πρωτεύον κλειδί. */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "log_id")
    private int logId;

    /** Ημερομηνία και ώρα του συμβάντος. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp", nullable = false)
    private Date timestamp;

    /** Αν η πρόσβαση εγκρίθηκε ή απορρίφθηκε. */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_granted", nullable = false)
    private PermissionType accessGranted;

    /** Είσοδος ή έξοδος. */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private AccessType accessType;

    /** Κάρτα που χρησιμοποιήθηκε. (Many-to-One, owning side) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id", nullable = false)
    private AccessCard accessCard;

    /** Checkpoint στο οποίο έγινε το συμβάν. (Many-to-One, owning side) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkpoint_fk", nullable = false)
    private Checkpoint checkpoint;

    // ================= Constructors =================

    /** Απαραίτητος default constructor για JPA. */
    public AccessLog() {
    }

    /**
     * Βασικός constructor που χρησιμοποιείται από το domain.
     * Θέτει την timestamp στην τρέχουσα στιγμή.
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
     * Helper setter για να κρατάμε αμφίδρομη τη σχέση
     * με την AccessCard (μέσω του accessLogs set).
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

        // Αν έχουμε id, συγκρίνουμε με βάση το id
        if (logId != 0 && accessLog.logId != 0) {
            return logId == accessLog.logId;
        }

        // Αλλιώς, “λογική” ισότητα – όχι τόσο κρίσιμη στα tests
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
