package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "access_card")
public class AccessCard {

    /** Ο μοναδικός αναγνωριστικός αριθμός της κάρτας (Primary Key). */
    @Id
    @Column(name = "card_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int cardId;

    /** Η ημερομηνία λήξης της κάρτας. */
    @Temporal(TemporalType.DATE)
    @Column(name = "expiration_date", nullable = false)
    private Date expirationDate;

    /** Η τρέχουσα κατάσταση της κάρτας (ACTIVE/INACTIVE). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status;

    // ⬇️ ΣΧΕΣΗ 1:1 (One-to-One) με User (Non-Owning Side)
    // mappedBy="accessCard" δείχνει στο πεδίο accessCard της κλάσης User (Owning Side).
    @OneToOne(mappedBy = "accessCard", fetch = FetchType.LAZY)
    private User user;

    // ⬇️ ΣΧΕΣΗ 1:Ν με Permission (Non-Owning Side - Συλλογή)
    @OneToMany(mappedBy = "accessCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Permission> permissions = new HashSet<>();

    // ⬇️ ΣΧΕΣΗ 1:Ν με AccessLog (Non-Owning Side - Συλλογή)
    @OneToMany(mappedBy = "accessCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AccessLog> accessLogs = new HashSet<>();

    // Default Constructor
    public AccessCard() {
        this.status = ActivityStatus.Inactive;
    }

    /**
     * Constructor που χρησιμοποιείται κατά την έκδοση της κάρτας.
     * @param expirationDate Η ημερομηνία λήξης.
     */
    public AccessCard(Date expirationDate) {
        this.expirationDate = expirationDate;
        this.status = ActivityStatus.Active; // Η κάρτα ξεκινάει ως ACTIVE
    }

    // ------------------- Business Methods -------------------

    /**
     * Ακυρώνει την κάρτα και την θέτει σε INACTIVE.
     * @throws IllegalStateException αν η κάρτα είναι ήδη ανενεργή.
     */
    public void deactivateCard() {
        if (this.status == ActivityStatus.Inactive) {
            throw new IllegalStateException("Card is already inactive.");
        }
        this.status = ActivityStatus.Inactive;

        // ΣΗΜΕΙΩΣΗ: Η ενημέρωση του σχετικού RegistrationRequest σε INACTIVE
        // πρέπει να γίνει στο Service Layer.
    }

    /**
     * Checks if the card is currently active and not expired.
     * @return true if the card is currently valid.
     */
    public boolean isValid() {
        // 1. Check status
        if (this.status != ActivityStatus.Active) {
            return false;
        }

        // 2. Check expiration date against the current time
        // The card is valid if the current time is BEFORE the expiration date.
        return this.expirationDate.after(new Date());
    }

    /**
     * Reactivates the card, setting its status to ACTIVE.
     * @throws IllegalStateException if the card is already active.
     */
    public void reactivateCard() {
        if (this.status == ActivityStatus.Active) {
            throw new IllegalStateException("Card is already active.");
        }
        this.status = ActivityStatus.Active;
    }

    // ------------------- Getters and Setters -------------------

    // Getters/Setters for simple fields
    public int getCardId() { return cardId; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    // Getters/Helpers for Relationships
    public User getUser() { return user; }

    // Helper Method για αμφίδρομη συνοχή (User - Σχέση 1:1)
    public void setUser(User user) {
        this.user = user;
        // Διασφαλίζει ότι ο User (Owning Side) δείχνει σε αυτήν την κάρτα
        if (user != null && user.getAccessCard() != this) {
            user.setAccessCard(this);
        }
    }

    public Set<Permission> getPermissions() { return permissions; }

    // Helper Method για αμφίδρομη συνοχή (ADD Permission)
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
        // Διασφαλίζει ότι το Permission (Owning Side) δείχνει σε αυτήν την κάρτα
        if (permission.getAccessCard() != this) {
            permission.setAccessCard(this);
        }
    }

    public Set<AccessLog> getAccessLogs() { return accessLogs; }

    // Helper Method για αμφίδρομη συνοχή (ADD AccessLog)
    public void addAccessLog(AccessLog accessLog) {
        this.accessLogs.add(accessLog);
        // Διασφαλίζει ότι το AccessLog (Owning Side) δείχνει σε αυτήν την κάρτα
        if (accessLog.getAccessCard() != this) {
            accessLog.setAccessCard(this);
        }
    }

    // ------------------- Equals and HashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessCard that = (AccessCard) o;
        return cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId);
    }
}