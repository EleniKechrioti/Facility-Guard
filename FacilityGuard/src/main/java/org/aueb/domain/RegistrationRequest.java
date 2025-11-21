package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "registration_request")
public class RegistrationRequest {

    /** Ο μοναδικός αναγνωριστικός αριθμός του αιτήματος (Primary Key). */
    @Id
    @Column(name = "registration_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int registrationId;

    /** Η ημερομηνία υποβολής του αιτήματος. */
    @Temporal(TemporalType.DATE)
    @Column(name = "request_date", nullable = false)
    private Date requestDate;

    /** Flag που δείχνει αν το αίτημα έχει εγκριθεί. */
    @Column(name = "approved", nullable = false)
    private boolean approved;

    /**
     * Η κατάσταση του αιτήματος (ACTIVE: αναμένεται ή έχει εγκριθεί/δίνει δικαίωμα κάρτας,
     * INACTIVE: απορρίφθηκε/ακυρώθηκε).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status;

    // ⬇️ ΣΧΕΣΗ MANY-TO-ONE με User (Non-Owning Side: Request, Owning Side: User - FK: user_fk)
    /**
     * Ο User που έκανε αυτό το αίτημα. Το Foreign Key βρίσκεται εδώ.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", referencedColumnName = "id", nullable = false)
    private User user;

    // Default Constructor
    public RegistrationRequest() {
        this.requestDate = new Date();
        this.approved = false;
        // Κάθε νέο αίτημα ξεκινά ως ACTIVE, ώστε να εμποδίζεται νέο submit.
        this.status = ActivityStatus.Active;
    }

    // ------------------- Business Methods -------------------

    /**
     * Ορίζει την κατάσταση έγκρισης του αιτήματος.
     * @param approved Η νέα κατάσταση (true/false).
     * @param actingUser Ο User που επιχειρεί την αλλαγή (πρέπει να είναι Administrator).
     * @throws SecurityException αν ο actingUser δεν είναι Administrator.
     * @throws IllegalStateException αν το αίτημα είναι INACTIVE.
     */
    public void setApprovedStatus(boolean approved, User actingUser) {
        // 1. Έλεγχος Ρόλου (Security Check)
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only users with the role 'ADMINISTRATOR' can modify the approval status.");
        }

        // 2. Έλεγχος Κατάστασης
        if (this.status == ActivityStatus.Inactive) {
            throw new IllegalStateException("Cannot change approval status for an INACTIVE request.");
        }

        this.approved = approved;

        // 3. Λογική Κατάστασης
        // Αν ο Administrator το απορρίπτει, το αίτημα κλείνει (INACTIVE),
        // απελευθερώνοντας τον User για νέα υποβολή.
        if (!approved) {
            this.status = ActivityStatus.Inactive;
        }
        // Αν γίνει approved=true, το status παραμένει ACTIVE,
        // υποδεικνύοντας ότι ο χρήστης έχει δικαίωμα για AccessCard.
    }

    /**
     * Ακυρώνει/Οριστικοποιεί το αίτημα ως INACTIVE.
     * Αυτό μπορεί να χρησιμοποιηθεί για να "κλείσει" ένα αίτημα,
     * π.χ., αν ο χρήστης αποσύρει την κάρτα του.
     */
    public void invalidateRequest(User actingUser) {
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only administrators can invalidate a request.");
        }
        this.status = ActivityStatus.Inactive;
        this.approved = false;
    }

    // ------------------- Getters and Setters -------------------

    public int getRegistrationId() { return registrationId; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public boolean isApproved() { return approved; }
    // Internal setter for JPA/Hibernate use only (Private ή Protected)
    private void setApproved(boolean approved) { this.approved = approved; }

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    public User getUser() { return user; }

    // Helper Method για αμφίδρομη συνοχή (User - Πλευρά Many)
    public void setUser(User user) {
        this.user = user;
        // Αν ο user δεν είναι null και δεν περιέχει ήδη αυτό το request, προσθέτουμε.
        if (user != null && !user.getRegistrationRequests().contains(this)) {
            user.addRegistrationRequest(this);
        }
    }

    // ------------------- Equals and HashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistrationRequest that = (RegistrationRequest) o;
        return registrationId == that.registrationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrationId);
    }
}