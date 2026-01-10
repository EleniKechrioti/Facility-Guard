package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;

import java.util.Objects;

@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @Column(name = "permission_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int permissionId;

    /** Permission type (AccessGranted/AccessDenied). */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_granted", nullable = false)
    private PermissionType accessGranted;

    /** Relationship MANY-TO-ONE with AccessCard (Owning Side)  */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id", nullable = false)
    private AccessCard accessCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_fk", nullable = false)
    private Area area;

    /** Default Constructor*/
    public Permission() {}

    /**
     * Constructor for new Permission object creation.
     */
    public Permission(PermissionType accessGranted, AccessCard accessCard, Area area) {
        this.accessGranted = accessGranted;
        setAccessCard(accessCard);
        setArea(area);
    }

    // ------------------- Business Method -------------------

    /**
     * Updates the PermissionType of the permission, with role checking.
     * @param newType The new PermissionType.
     * @param actingUser The User attempting the change.
     * @throws SecurityException if the actingUser is not an Administrator.
     */
    public void updatePermissionType(PermissionType newType, User actingUser) {
        /** Role Check  */
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only users with the role 'ADMINISTRATOR' can modify the permission type.");
        }

        /** Applying the change (allowed)  */
        this.accessGranted = newType;
    }

    /**
     * Permanently invalidates the permission, typically when the AccessCard expires or is revoked.
     * This should be done only by an Administrator.
     * @param actingUser The User attempting the invalidation (must be Administrator).
     * @throws SecurityException if the actingUser is not an Administrator.
     */
    public void invalidate(User actingUser) {
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only administrators can invalidate a permission.");
        }
        this.accessGranted = PermissionType.AccessDenied;
    }

    // ------------------- Getters και Helper Setters -------------------

    public int getPermissionId() { return permissionId; }

    public PermissionType getAccessGranted() { return accessGranted; }

    public AccessCard getAccessCard() { return accessCard; }

    public void setAccessCard(AccessCard accessCard) {
        this.accessCard = accessCard;
        if (accessCard != null && !accessCard.getPermissions().contains(this)) {
            accessCard.addPermission(this);
        }
    }

    /**
     * Returns the Area in which the permission is referenced.
     */
    public Area getArea() {
        return area;
    }

    /**
     * Sets the Area.
     */
    public void setArea(Area area) {
        this.area = area;

    }

    // ------------------- Equals and HashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return permissionId == that.permissionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId);
    }
}