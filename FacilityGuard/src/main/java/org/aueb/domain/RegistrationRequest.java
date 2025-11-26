package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "registration_request")
public class RegistrationRequest {

    /** Primary Key */
    @Id
    @Column(name = "registration_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int registrationId;

    /** The submission date of the request. */
    @Temporal(TemporalType.DATE)
    @Column(name = "request_date", nullable = false)
    private Date requestDate;

    /** Flag that indicates whether the request has been approved. */
    @Column(name = "approved", nullable = false)
    private boolean approved;

    /**
     * The status of the request (Active: pending or has been approved/grants card entitlement,
     * Inactive: rejected/cancelled).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActivityStatus status;

    // ⬇️ ΣΧΕΣΗ MANY-TO-ONE με User (Non-Owning Side: Request, Owning Side: User - FK: user_fk)
    /**
     * Relationship Many-to-One with User (RegistationRequest is the owning side)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_fk", referencedColumnName = "id", nullable = false)
    private User user;

    /** Default Constructor   */
    public RegistrationRequest() {
        this.requestDate = new Date();
        this.approved = false;
        /** Every new request starts as Active, in order to prevent a new submission. */
        this.status = ActivityStatus.Active;
    }

    // ------------------- Business Methods -------------------

    /**
     * Sets the approval status of the request.
     * @param approved The new status (true/false).
     * @param actingUser The User attempting the change (must be an Administrator).
     * @throws SecurityException if the actingUser is not an Administrator.
     * @throws IllegalStateException if the request is Inactive.
     */
    public void setApprovedStatus(boolean approved, User actingUser) {
        /** Role Check (Security Check)  */
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only users with the role 'ADMINISTRATOR' can modify the approval status.");
        }

        /**   Status Check  */
        if (this.status == ActivityStatus.Inactive) {
            throw new IllegalStateException("Cannot change approval status for an INACTIVE request.");
        }

        this.approved = approved;

        /** Status Logic
        * If the Administrator rejects it, the request is closed (Inactive),
         enabling the User for a new submission.       */
        if (!approved) {
            this.status = ActivityStatus.Inactive;
        }
        /** If approved=true, the status remains active,
          indicating that the user is entitled to an AccessCard.   */
    }

    /**
     * Cancels/Finalizes the request as INACTIVE.
     * This can be used to "close" a request,
     * e.g., if the user withdraws their card
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

    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus status) { this.status = status; }

    public User getUser() { return user; }

    /** Helper Method for bidirectional consistency (User - Many Side)   */
    public void setUser(User user) {
        this.user = user;
        /** If the user is not null and does not already contain this request, we add it.   */
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