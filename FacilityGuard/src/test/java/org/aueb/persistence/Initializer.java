package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.aueb.domain.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.UserType;
import java.util.Objects;

public class Initializer {

    private EntityManager em;
    private static Building testBuilding;

    public Initializer() {
        em = JPAUtil.getCurrentEntityManager();
    }

    private void eraseData() {
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        em.createNativeQuery("delete from checkpoints");
        em.createNativeQuery("delete from permissions");
        em.createNativeQuery("delete from areas");
        em.createNativeQuery("delete from buildings");
        em.createNativeQuery("delete from access_card");
        em.createNativeQuery("delete from registration_request");
        em.createNativeQuery("delete from users").executeUpdate();

        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

        tx.commit();
    }

    public void prepareData() {

        eraseData();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        User user = new User("userTest", "password", "John", "Doe", "johndoe@email.com", UserType.Administrator);

        Address addr = new Address("Test Street", "1", "Test City", "10000", "Greece");
        testBuilding = new Building("Test Headquarters", addr);

        Area serverRoom = new Area("Server Room 101", testBuilding);

        // Checkpoint (Συνδεδεμένο με την Area)
        Checkpoint cp1 = new Checkpoint("Server Room Reader");

        // 4. Σύνδεση Σχέσεων

        // Building <-> Area
        testBuilding.addArea(serverRoom);

        // Area <-> Checkpoint
        serverRoom.addCheckpoint(cp1);

        em.persist(user);
        em.persist(testBuilding); // Persist the parent entity

        tx.commit();

        em.close();
    }

    /**
     * Retrieves the Building entity that was persisted by the Initializer.
     * This allows test methods to access the shared, persistent data.
     * @return The persistent Building object.
     */
    public static Building getPersistedBuilding() {
        return Objects.requireNonNull(testBuilding, "Test data (Building) was not initialized correctly.");
    }
}