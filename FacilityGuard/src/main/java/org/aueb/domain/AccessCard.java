package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;

import java.util.Date;
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

    // ⬇️ ΣΧΕΣΗ 1:1 (One-to-One) με User (Non-Owning Side - ΧΩΡΙΣ @JoinColumn)
    // mappedBy="accessCard" δείχνει στο πεδίο accessCard της κλάσης User (Owning Side).
    @OneToOne(mappedBy = "accessCard", fetch = FetchType.LAZY)
    private User user;

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

        // ΣΗΜΕΙΩΣΗ: Η ενημέρωση του αντίστοιχου RegistrationRequest σε INACTIVE
        // πρέπει να γίνει στο Service Layer για να βρεθεί το σωστό αίτημα.
    }

    // ------------------- Getters and Setters -------------------

    public int getCardId() { return cardId; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    public User getUser() { return user; }

    // Helper Method για αμφίδρομη συνοχή (User)
    public void setUser(User user) {
        this.user = user;
        // Διασφαλίζει ότι ο User (Owning Side) δείχνει σε αυτήν την κάρτα
        if (user != null && user.getAccessCard() != this) {
            user.setAccessCard(this);
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