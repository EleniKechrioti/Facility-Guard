package org.aueb.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;
import org.junit.jupiter.api.*;

@QuarkusTest
public class UserManagementServiceTest {

    @Inject
    UserManagementService service;

    @Inject
    UserRepository userRepo;

    @Inject
    AccessCardRepository cardRepo;

    @Inject
    AreaRepository areaRepo;

    @Inject
    BuildingRepository buildingRepo;

    @Inject
    PermissionRepository permissionRepo;


    @Inject
    EntityManager em;

    private Integer userId;
    private Integer adminId;
    private Integer areaId;

    /* ================= SETUP ================= */

    @BeforeEach
    @Transactional
    void setup() {

        User admin = new User(
                "admin",
                "pass",
                "Admin",
                "User",
                "admin@test.com",
                UserType.Administrator
        );
        userRepo.persist(admin);
        adminId = admin.getUserId();

        User user = new User(
                "emp",
                "pass",
                "John",
                "Doe",
                "john@test.com",
                UserType.Employee
        );
        userRepo.persist(user);
        userId = user.getUserId();

        Building building = new Building("HQ", null);
        buildingRepo.persist(building);

        Area area = new Area("Server Room", building);
        areaRepo.persist(area);
        areaId = area.getAreaId();
    }

    /* ================= CLEANUP ================= */

    @AfterEach
    @Transactional
    void cleanup() {
        // ⬅️ ΣΩΣΤΗ ΣΕΙΡΑ (αποφυγή FK constraint violations)
        userRepo.deleteAll();
        permissionRepo.deleteAll();
        cardRepo.deleteAll();
        areaRepo.deleteAll();
        buildingRepo.deleteAll();
    }

    /* ================= issueCardToUser ================= */

    @Test
    @Transactional
    void issueCard_createsNewCard() {

        AccessCard card = service.issueCardToUser(userId, 6);

        Assertions.assertNotNull(card);
        Assertions.assertEquals(ActivityStatus.Active, card.getStatus());
        Assertions.assertNotNull(card.getExpirationDate());

        User user = userRepo.findById(userId);
        Assertions.assertEquals(card.getCardId(), user.getAccessCard().getCardId());
    }

    @Test
    @Transactional
    void issueCard_deactivatesOldCard() {

        AccessCard first = service.issueCardToUser(userId, 3);
        AccessCard second = service.issueCardToUser(userId, 6);

        Assertions.assertEquals(ActivityStatus.Inactive, first.getStatus());
        Assertions.assertEquals(ActivityStatus.Active, second.getStatus());
    }

    @Test
    void issueCard_userNotFound() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                service.issueCardToUser(9999, 3)
        );
    }

    /* ================= grantPermission ================= */

    @Test
    @Transactional
    void grantPermission_success() {

        service.issueCardToUser(userId, 6);
        User admin = userRepo.findById(adminId);

        service.grantPermission(userId, areaId, admin);

        em.flush();
        em.clear();

        User user = userRepo.findById(userId);
        Permission p = user.getAccessCard()
                .getPermissions()
                .iterator()
                .next();

        Assertions.assertEquals(PermissionType.AccessGranted, p.getAccessGranted());
        Assertions.assertEquals(areaId, p.getArea().getAreaId());
    }

    @Test
    @Transactional
    void grantPermission_notAdmin() {

        service.issueCardToUser(userId, 6);
        User notAdmin = userRepo.findById(userId);

        Assertions.assertThrows(SecurityException.class, () ->
                service.grantPermission(userId, areaId, notAdmin)
        );
    }

    @Test
    @Transactional
    void grantPermission_invalidCard() {

        User admin = userRepo.findById(adminId);

        Assertions.assertThrows(IllegalStateException.class, () ->
                service.grantPermission(userId, areaId, admin)
        );
    }

    /* ================= revokePermission ================= */

    @Test
    @Transactional
    void revokePermission_setsDenied() {

        service.issueCardToUser(userId, 6);
        User admin = userRepo.findById(adminId);

        service.grantPermission(userId, areaId, admin);
        service.revokePermission(userId, areaId, admin);

        Permission p = userRepo.findById(userId)
                .getAccessCard()
                .getPermissions()
                .iterator()
                .next();

        Assertions.assertEquals(PermissionType.AccessDenied, p.getAccessGranted());
    }

    /* ================= deactivateUser ================= */

    @Test
    @Transactional
    void deactivateUser_disablesCard() {

        service.issueCardToUser(userId, 6);
        service.deactivateUser(userId);

        AccessCard card = userRepo.findById(userId).getAccessCard();
        Assertions.assertEquals(ActivityStatus.Inactive, card.getStatus());
    }
}
