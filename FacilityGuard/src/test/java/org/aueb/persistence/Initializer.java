package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.aueb.domain.*;

public class Initializer {

    private EntityManager em;

    public Initializer() {
        em = JPAUtil.getCurrentEntityManager();
    }

    private void eraseData() {
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        em.createNativeQuery("delete from users").executeUpdate();

        tx.commit();
    }

    public void prepareData() {

        eraseData();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        User user = new User("userTest", "password", "John", "Doe", "johndoe@email.com");
        em.persist(user);

        tx.commit();

        em.close();
    }
}