package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.AccessType;
import java.util.Date;
import java.util.Objects;

/**
 * Represents a log entry every time an AccessCard is used to attempt access.
 * This entity is mapped to the "ACCESS_LOG" table.
 */
@Entity
@Table(name = "access_log")
public class AccessLog {

    /** Ο μοναδικός αναγνωριστικός αριθμός της καταγραφής (Primary Key). */
    @Id
    @Column(name = "log_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int logId;

    /** Η ακριβής ώρα και ημερομηνία της απόπειρας πρόσβασης. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp", nullable = false)
    private Date timestamp;

    /** Το αποτέλεσμα της απόπειρας πρόσβασης (Granted/Denied). */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_granted", nullable = false)
    private PermissionType accessGranted;

    /** Ο τύπος πρόσβασης (In/Out). */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private AccessType accessType;

    // ⬇️ ΣΧΕΣΗ MANY-TO-ONE με AccessCard (Owning Side)
    /**
     * Η AccessCard που χρησιμοποίησε ο χρήστης.
     * Το Foreign Key (card_fk) μπαίνει σε αυτόν τον πίνακα.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id", nullable = false)
    private AccessCard accessCard;

    // Default Constructor
    public AccessLog() {
        this.timestamp = new Date(); // Ορίζει την τρέχουσα ώρα/ημερομηνία
    }

    /**
     * Constructor για τη δημιουργία νέου log entry.
     */
    public AccessLog(PermissionType accessGranted, AccessType accessType, AccessCard accessCard) {
        this(); // Καλέι τον default constructor για να θέσει το timestamp
        this.accessGranted = accessGranted;
        this.accessType = accessType;
        // Χρησιμοποιούμε τον helper για αμφίδρομη σύνδεση
        setAccessCard(accessCard);
    }

    // ------------------- Getters and Setters -------------------

    public int getLogId() { return logId; }
    public Date getTimestamp() { return timestamp; }
    // Δεν παρέχουμε setter για το timestamp, καθώς ορίζεται κατά τη δημιουργία

    public PermissionType getAccessGranted() { return accessGranted; }
    public void setAccessGranted(PermissionType accessGranted) { this.accessGranted = accessGranted; }

    public AccessType getAccessType() { return accessType; }
    public void setAccessType(AccessType accessType) { this.accessType = accessType; }

    public AccessCard getAccessCard() { return accessCard; }

    // Helper Method για αμφίδρομη συνοχή (AccessCard - Πλευρά Many)
    public void setAccessCard(AccessCard accessCard) {
        this.accessCard = accessCard;
        // Διασφαλίζει ότι η AccessCard (πλευρά One) περιέχει αυτό το log στη συλλογή της
        if (accessCard != null && !accessCard.getAccessLogs().contains(this)) {
            accessCard.addAccessLog(this);
        }
    }

    // ------------------- Equals and HashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessLog that = (AccessLog) o;
        return logId == that.logId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }
}