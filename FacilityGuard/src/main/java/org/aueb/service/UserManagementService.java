package org.aueb.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.AccessCardRepository;
import org.aueb.persistence.AreaRepository;
import org.aueb.persistence.UserRepository;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
public class UserManagementService {

    @Inject
    UserRepository userRepo;

    @Inject
    AccessCardRepository cardRepo;

    @Inject
    AreaRepository areaRepo;

    /**
     * Εκδίδει νέα κάρτα για έναν χρήστη.
     * @param userId Το ID του χρήστη.
     * @param monthsValid Διάρκεια ισχύος σε μήνες.
     * @return Η νέα κάρτα.
     */
    @Transactional
    public AccessCard issueCardToUser(Integer userId, int monthsValid) {
        User user = userRepo.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User with ID " + userId + " not found.");
        }

        // 1. Αν ο χρήστης έχει ήδη ενεργή κάρτα, την ακυρώνουμε
        if (user.getAccessCard() != null) {
            AccessCard oldCard = user.getAccessCard();
            if (oldCard.getStatus() == ActivityStatus.Active) {
                oldCard.deactivateCard();
            }
        }

        // 2. Υπολογισμός ημερομηνίας λήξης
        Date expirationDate = Date.from(LocalDate.now().plusMonths(monthsValid)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        // 3. Δημιουργία Νέας Κάρτας
        AccessCard newCard = new AccessCard(expirationDate);

        // 4. Σύνδεση με τον Χρήστη
        newCard.setUser(user);

        // 5. Αποθήκευση
        cardRepo.persist(newCard);

        return newCard;
    }

    /**
     * Δίνει πρόσβαση σε μια περιοχή για τον συγκεκριμένο χρήστη.
     * @param userId Το ID του χρήστη που θα πάρει την πρόσβαση.
     * @param areaId Το ID της περιοχής (Area).
     * @param actingUser Ο διαχειριστής που εκτελεί την ενέργεια (απαραίτητο για updatePermissionType).
     */
    @Transactional
    public void grantPermission(Integer userId, Integer areaId, User actingUser) {
        // 1. Έλεγχος: Ο actingUser πρέπει να είναι Admin (βάσει της λογικής του Permission class)
        if (actingUser == null || actingUser.getUserType() != UserType.Administrator) {
            throw new SecurityException("Only Administrators can grant permissions.");
        }

        // 2. Βρίσκουμε Χρήστη και Ζώνη
        User targetUser = userRepo.findById(userId);
        Area area = areaRepo.findById(areaId);

        if (targetUser == null) throw new IllegalArgumentException("Target user not found");
        if (area == null) throw new IllegalArgumentException("Area not found");

        // 3. Παίρνουμε την κάρτα του χρήστη
        AccessCard card = targetUser.getAccessCard();

        if (card == null || !card.isValid()) {
            throw new IllegalStateException("User does not have a valid active card.");
        }

        // 4. Ψάχνουμε αν υπάρχει ήδη Permission για αυτή τη ζώνη
        Optional<Permission> existingPerm = card.getPermissions().stream()
                .filter(p -> p.getArea().getAreaId() == area.getAreaId())
                .findFirst();

        if (existingPerm.isPresent()) {
            // Υπάρχει ήδη. Αν είναι Denied, το κάνουμε Granted.
            Permission p = existingPerm.get();
            if (p.getAccessGranted() != PermissionType.AccessGranted) { // Υποθέτω ότι το Enum είναι AccessGranted
                p.updatePermissionType(PermissionType.AccessGranted, actingUser);
                // Δεν χρειάζεται persist, είναι managed object
            }
        } else {
            // Δεν υπάρχει, φτιάχνουμε καινούργιο
            // Ο Constructor καλεί τα setters που ενημερώνουν και την AccessCard list
            Permission newPermission = new Permission(PermissionType.AccessGranted, card, area);

            // Λόγω CascadeType.ALL στην AccessCard, θα σωθεί αυτόματα στο τέλος του transaction
            cardRepo.persist(card);
        }
    }

    /**
     * Ανακαλεί την πρόσβαση σε μια περιοχή (Invalidate).
     */
    @Transactional
    public void revokePermission(Integer userId, Integer areaId, User actingUser) {
        User targetUser = userRepo.findById(userId);
        if (targetUser == null) return;

        AccessCard card = targetUser.getAccessCard();
        if (card == null) return;

        // Βρίσκουμε το permission
        card.getPermissions().stream()
                .filter(p -> p.getArea().getAreaId() == areaId)
                .findFirst()
                .ifPresent(p -> p.invalidate(actingUser)); // Καλεί τη μέθοδο της Entity
    }

    /**
     * Απενεργοποίηση χρήστη και της κάρτας του.
     */
    @Transactional
    public void deactivateUser(Integer userId) {
        User user = userRepo.findById(userId);
        if (user == null) return;

        AccessCard card = user.getAccessCard();
        if (card != null && card.getStatus() == ActivityStatus.Active) {
            card.deactivateCard();
        }
    }
}