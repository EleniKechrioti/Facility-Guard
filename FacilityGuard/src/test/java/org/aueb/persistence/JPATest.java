package org.aueb.persistence;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.Building;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class JPATest {
    @Inject
    EntityManager em;

    @Transactional
    @BeforeEach
    public void initDb()  {
        em.createNativeQuery("DELETE FROM registration_request").executeUpdate();
        em.createNativeQuery("DELETE FROM alerts").executeUpdate();
        em.createNativeQuery("DELETE FROM access_log").executeUpdate();
        em.createNativeQuery("DELETE FROM checkpoints").executeUpdate();
        em.createNativeQuery("DELETE FROM permission").executeUpdate();
        em.createNativeQuery("UPDATE users SET card_fk = NULL").executeUpdate(); // Σπάμε τον δεσμό
        em.createNativeQuery("DELETE FROM access_card").executeUpdate();
        em.createNativeQuery("DELETE FROM users").executeUpdate();

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("import.sql");
        String sql = convertStreamToString(in);
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        em.createNativeQuery(sql).executeUpdate();
    }


    private String convertStreamToString(InputStream in) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(in,"UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}