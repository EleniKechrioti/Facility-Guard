package org.aueb.persistence;

import org.aueb.domain.*;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class RegistrationRequestJPATest extends JPATest{

    @Test
    public void listRegistrationRequests(){
        List<RegistrationRequest> allRequests = em.createQuery(
                        "SELECT r FROM RegistrationRequest r", RegistrationRequest.class)
                .getResultList();
        assertEquals(2, allRequests.size());
    }


    @Test
    public void listRegistrationRequestsbyUserEmail(){
        List<RegistrationRequest> employeeRequests = em.createQuery(
                        "SELECT r FROM RegistrationRequest r JOIN r.user u WHERE u.email = :email",
                        RegistrationRequest.class)
                .setParameter("email", "johndoe@email.com")
                .getResultList();
        assertEquals(1, employeeRequests.size());

    }


}
