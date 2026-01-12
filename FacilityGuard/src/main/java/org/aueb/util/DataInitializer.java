package org.aueb.util;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import org.aueb.domain.*;
import org.aueb.persistence.*;
import org.aueb.util.Address;
import org.aueb.util.enumerations.AccessType;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.PermissionType;
import org.aueb.util.enumerations.UserType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@ApplicationScoped
public class DataInitializer {

    @Inject UserRepository userRepo;
    @Inject BuildingRepository buildingRepo;
    @Inject AreaRepository areaRepo;
    @Inject CheckpointRepository checkpointRepo;
    @Inject AccessCardRepository cardRepo;
    @Inject PermissionRepository permissionRepo;
    @Inject AccessLogRepository logRepo;
    @Inject AlertRepository alertRepo;
    @Inject EntityManager em;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (userRepo.count() > 0) {
            return;
        }

        System.out.println(">>> Initializing Rich Data Scenario...");

        // Infrastructure (Building & Areas & Checkpoints)
        Building mainB = new Building("AUEB HQ", new Address("Patission", "76", "Athens", "10434", "GR"));
        buildingRepo.persist(mainB);

        // --- Areas ---
        Area parking = new Area("Parking (Level -1)", mainB);
        Area lobby = new Area("Main Lobby (Ground)", mainB);
        Area offices = new Area("Offices (Level 1)", mainB);
        Area serverRoom = new Area("Server Room (Secure)", mainB);

        // --- Topology (Neighbors) ---
        // The Lobby is connected to Parking and Offices
        lobby.addNeighbor(parking);
        lobby.addNeighbor(offices);
        // Offices are connected to Server Room
        offices.addNeighbor(serverRoom);

        areaRepo.persist(parking);
        areaRepo.persist(lobby);
        areaRepo.persist(offices);
        areaRepo.persist(serverRoom);

        // --- Checkpoints ---
        Checkpoint cpParking = createCheckpoint("Gate A (Parking)", parking);
        Checkpoint cpLobbyFront = createCheckpoint("Main Entrance", lobby);
        Checkpoint cpLobbyElevator = createCheckpoint("Lobby Elevator", lobby); // Εσωτερικό checkpoint
        Checkpoint cpOfficeDoor = createCheckpoint("Office Corridor", offices);
        Checkpoint cpServerBio = createCheckpoint("Server Retina Scan", serverRoom);

        // USERS & REQUESTS & CARDS

        // --- ADMIN ---
        User admin = new User("admin", "admin", "System", "Admin", "admin@sys.com", UserType.Administrator);
        userRepo.persist(admin);
        createRequest(admin, "Initial Admin Setup", true); // Auto-approved

        // USER 1
        User alice = new User("alice", "1234", "Alice", "Wonder", "alice@dev.com", UserType.Employee);
        userRepo.persist(alice);
        createRequest(alice, "Request for Senior Role", true); // Approved

        AccessCard cAlice = createCard(alice);
        grant(cAlice, lobby);
        grant(cAlice, parking);
        grant(cAlice, offices);
        grant(cAlice, serverRoom);

        // USER 2
        User john = new User("john", "1234", "John", "Doe", "john@acc.com", UserType.Employee);
        userRepo.persist(john);
        createRequest(john, "Request for Accounting", true); // Approved

        AccessCard cJohn = createCard(john);
        grant(cJohn, parking);
        grant(cJohn, lobby);
        grant(cJohn, offices);
        deny(cJohn, serverRoom);

        // USER 3
        User maria = new User("maria", "1234", "Maria", "Newbie", "maria@intern.com", UserType.Employee);
        userRepo.persist(maria);
        createRequest(maria, "Internship Access", true); // Approved

        AccessCard cMaria = createCard(maria);
        grant(cMaria, lobby);

        User costas = new User("costas", "1234", "Costas", "Guest", "costas@gmail.com", UserType.Visitor);
        userRepo.persist(costas);
        createRequest(costas, "Visit for interview", false);

        System.out.println(">>> 🕒 Generating Historical Logs...");

        createLog(cAlice, cpParking, AccessType.In, true, 4, ChronoUnit.HOURS);
        createLog(cAlice, cpLobbyFront, AccessType.In, true, 3, ChronoUnit.HOURS, 55);
        createLog(cAlice, cpOfficeDoor, AccessType.In, true, 3, ChronoUnit.HOURS, 50);

        AccessLog logJohnFail = createLog(cJohn, cpServerBio, AccessType.In, false, 2, ChronoUnit.HOURS);

        createAlert(logJohnFail, "Security Violation: User attempted access to Explicitly DENIED Area (Server Room).");

        AccessLog logMariaFail = createLog(cMaria, cpOfficeDoor, AccessType.In, false, 1, ChronoUnit.HOURS);

        createAlert(logMariaFail, "Authorization Failure: No permission found for Area 'Offices'.");

        System.out.println("\n--- ✅ DATA GENERATION COMPLETE ---");
        System.out.println("Checkpoints IDs:");
        System.out.printf("  [%d] Parking Gate\n", cpParking.getCheckpointId());
        System.out.printf("  [%d] Lobby Entrance\n", cpLobbyFront.getCheckpointId());
        System.out.printf("  [%d] Office Door\n", cpOfficeDoor.getCheckpointId());
        System.out.printf("  [%d] Server Scanner\n", cpServerBio.getCheckpointId());

        System.out.println("\nUsers & Cards:");
        System.out.printf("  Alice (Full Access) -> Card ID: %d\n", cAlice.getCardId());
        System.out.printf("  John (No Server)    -> Card ID: %d\n", cJohn.getCardId());
        System.out.printf("  Maria (Lobby Only)  -> Card ID: %d\n", cMaria.getCardId());
        System.out.printf("  Costas (Pending)    -> No Card (User ID: %d)\n", costas.getUserId());
        System.out.println("-----------------------------------\n");
    }


    private Checkpoint createCheckpoint(String name, Area area) {
        Checkpoint cp = new Checkpoint(name);
        cp.setArea(area);
        checkpointRepo.persist(cp);
        return cp;
    }

    private AccessCard createCard(User user) {
        Date expDate = Date.from(Instant.now().plus(730, ChronoUnit.DAYS));
        AccessCard card = new AccessCard(expDate);
        card.setUser(user); // Συνδέουμε με χρήστη
        cardRepo.persist(card);
        return card;
    }

    private void createRequest(User user, String desc, boolean approved) {
        RegistrationRequest req = new RegistrationRequest();
        req.setUser(user);
        req.setRequestDate(new Date());
        req.setRequestDate(new Date());

        if (approved) {
            req.setStatus(ActivityStatus.Active);
        } else {
            req.setStatus(ActivityStatus.Inactive); // Inactive = Pending
        }

        em.persist(req);
    }

    private void grant(AccessCard card, Area area) {
        Permission p = new Permission(PermissionType.AccessGranted, card, area);
        permissionRepo.persist(p);
    }

    private void deny(AccessCard card, Area area) {
        Permission p = new Permission(PermissionType.AccessDenied, card, area);
        permissionRepo.persist(p);
    }

    private AccessLog createLog(AccessCard card, Checkpoint cp, AccessType type, boolean granted, long minusAmount, ChronoUnit unit) {
        return createLog(card, cp, type, granted, minusAmount, unit, 0);
    }

    private AccessLog createLog(AccessCard card, Checkpoint cp, AccessType type, boolean granted, long minusAmount, ChronoUnit unit, long minusMinutes) {
        PermissionType result = granted ? PermissionType.AccessGranted : PermissionType.AccessDenied;

        AccessLog log = new AccessLog(result, type, card, cp);

        // Φτιάχνουμε χρόνο στο παρελθόν
        Instant pastTime = Instant.now().minus(minusAmount, unit).minus(minusMinutes, ChronoUnit.MINUTES);
        log.setTimestamp(Date.from(pastTime));

        logRepo.persist(log);
        return log;
    }

    private void createAlert(AccessLog log, String reason) {
        Alert alert = new Alert(new Date(), reason); // Current time for alert creation usually, or log time
        alert.setAccessLog(log);
        alertRepo.persist(alert);
    }
}
