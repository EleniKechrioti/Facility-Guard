package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class JPAUtilTest {

    @Test
    public void testGetCurrentEntityManager(){
        EntityManager em = JPAUtil.getCurrentEntityManager();
        assertNotNull(em);

    }
}
