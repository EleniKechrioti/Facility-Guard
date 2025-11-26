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

    /** Primary Key */
    @Id
    @Column(name = "card_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int cardId;


    @Temporal(TemporalType.DATE)
    @Column(name = "expiration_date", nullable = false)
    private Date expirationDate;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status;

    /** Relationship One-to-One with User (User is the owning side)
     */
    @OneToOne(mappedBy = "accessCard", fetch = FetchType.LAZY)
    private User user;

    /** Relationship One-to-Many with Permission (Permission is the owning side)
      */
    @OneToMany(mappedBy = "accessCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Permission> permissions = new HashSet<>();

    /** Relationship One-to-Many with AccessLog (AccessLog is the owning side)
      */
    @OneToMany(mappedBy = "accessCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<AccessLog> accessLogs = new HashSet<>();

    /** Default Constructor
     */
    public AccessCard() {
        this.status = ActivityStatus.Inactive;
    }

    /** Constructor used when Access Card is issued
     */
    public AccessCard(Date expirationDate) {
        this.expirationDate = expirationDate;
        this.status = ActivityStatus.Active; /** starts with ACTIVE */
    }

    // ------------------- Business Methods -------------------

    /**
     * Deactivates card and sets it as inactive
     */
    public void deactivateCard() {
        if (this.status == ActivityStatus.Inactive) {
            throw new IllegalStateException("Card is already inactive.");
        }
        this.status = ActivityStatus.Inactive;

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

        /** Check expiration date against the current time
        *The card is valid if the current time is BEFORE the expiration date.  */
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

    /** Getters/Setters for simple fields */
    public int getCardId() { return cardId; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    /** Getters/Setters for Relationships  */
    public User getUser() { return user; }

    /** Helper Method for bidirectional consistency (User - 1:1 Relationship)  */
    public void setUser(User user) {
        this.user = user;
        /** Ensures that the User (Owning Side) points to this card. */
        if (user != null && user.getAccessCard() != this) {
            user.setAccessCard(this);
        }
    }

    public Set<Permission> getPermissions() { return permissions; }

    /** Helper Method for bidirectional consistency (ADD Permission)  */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
        /** Διασφαλίζει ότι το Permission (Owning Side) δείχνει σε αυτήν την κάρτα  */
        if (permission.getAccessCard() != this) {
            permission.setAccessCard(this);
        }
    }

    public Set<AccessLog> getAccessLogs() { return accessLogs; }

    /** Helper Method for bidirectional consistency (ADD AccessLog)  */
    public void addAccessLog(AccessLog accessLog) {
        this.accessLogs.add(accessLog);
        /** Ensures that the AccessLog (Owning Side) points to this card  */
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