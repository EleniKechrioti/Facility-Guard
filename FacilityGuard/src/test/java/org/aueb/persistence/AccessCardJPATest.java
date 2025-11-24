package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.aueb.domain.AccessCard;
import org.aueb.domain.*;

import org.aueb.util.enumerations.ActivityStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AccessCardJPATest extends JPATest{



    @Test
    public void listallCards(){
        List<AccessCard> result= em.createQuery("SELECT c FROM AccessCard c", AccessCard.class)
                .getResultList();
        assertEquals(1,result.size());

    }



    @Test
    public void fetchAccessCardWithPermissionAccessLogByUsername(){
        AccessCard retrievedCard = em.createQuery("SELECT c FROM AccessCard c " +
                "JOIN FETCH c.permissions p " +
                "JOIN FETCH c.accessLogs a " +
                "JOIN c.user u " +
                "WHERE u.username = :username",AccessCard.class)
        .setParameter("username", "admin.auth")
        .getSingleResult();


        assertNotNull(retrievedCard);

        assertEquals(ActivityStatus.Active, retrievedCard.getStatus());

        assertEquals(2, retrievedCard.getPermissions().size());

        assertEquals(1, retrievedCard.getAccessLogs().size());

        assertNotNull(retrievedCard.getUser());

        assertEquals("admin.auth", retrievedCard.getUser().getUsername());


    }
}
