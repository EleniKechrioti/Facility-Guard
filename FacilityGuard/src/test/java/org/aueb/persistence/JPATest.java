package org.aueb.persistence;

import jakarta.persistence.EntityManager;
import org.aueb.domain.Building;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class JPATest {
    protected EntityManager em;
    protected Building testBuilding;

    @BeforeEach
    public void setup() {

        Initializer initializer = new Initializer();
        initializer.prepareData();
        this.testBuilding = Initializer.getPersistedBuilding();

        this.em = JPAUtil.getCurrentEntityManager();

    }

    @AfterEach
    public void tearDown() {
        em.close();
    }
}