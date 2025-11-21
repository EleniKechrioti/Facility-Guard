package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.enumerations.UserType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    /** Ο μοναδικός αναγνωριστικός αριθμός του χρήστη (Primary Key). */
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int userId;

    /** Το μοναδικό username για login. */
    @Column(nullable = false, unique = true)
    private String username;

    /** Ο κωδικός πρόσβασης. */
    @Column(nullable = false)
    private String password;

    /** Όνομα. */
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    /** Επώνυμο. */
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    /** Email. */
    @Column(name = "email", length = 50, nullable = false)
    private String email;

    /** Ο ρόλος του χρήστη (ADMINISTRATOR, EMPLOYEE, VISITOR). */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    // ⬇️ ΣΧΕΣΗ 1:Ν (One-to-Many) με RegistrationRequest (Non-Owning Side)
    // Το Foreign Key (user_fk) βρίσκεται στην κλάση RegistrationRequest.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RegistrationRequest> registrationRequests = new HashSet<>();

    // ⬇️ ΣΧΕΣΗ 1:1 (One-to-One) με AccessCard (Owning Side - ΕΔΩ ΜΠΑΙΝΕΙ το FK)
    // card_fk: το Foreign Key στον πίνακα users που δείχνει στον πίνακα access_card
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id")
    private AccessCard accessCard;

    // Default Constructor
    public User() {}

    // Parameterized Constructor
    public User(String username, String password, String firstName, String lastName, String email, UserType userType){
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userType = userType;
    }

    // ------------------- Business Methods -------------------

    /**
     * Δημιουργεί και υποβάλλει ένα νέο αίτημα εγγραφής.
     * @throws IllegalStateException εάν ο χρήστης έχει ήδη ενεργό αίτημα (status=ACTIVE).
     */
    public RegistrationRequest submitRegistrationRequest() {
        // 1. ΕΛΕΓΧΟΣ BUSINESS RULE: Βρίσκει αν υπάρχει έστω και ένα αίτημα με status=ACTIVE
        boolean hasActiveRequest = this.registrationRequests.stream()
                .anyMatch(r -> r.getStatus() == ActivityStatus.Active);

        if (hasActiveRequest) {
            throw new IllegalStateException("User " + this.username + " already has an active registration request. Cannot submit a new one.");
        }

        // 2. Δημιουργία νέου αιτήματος (ξεκινάει ως ACTIVE/approved=false)
        RegistrationRequest newRequest = new RegistrationRequest();

        // 3. Χρήση του helper για αμφίδρομη σύνδεση
        addRegistrationRequest(newRequest);

        return newRequest;
    }

    /**
     * Εκδίδει μία νέα AccessCard στον User, αν υπάρχει ACTIVE/APPROVED αίτημα.
     * @param expirationDate Η ημερομηνία λήξης της κάρτας.
     * @return Η εκδοθείσα AccessCard.
     * @throws IllegalStateException αν δεν υπάρχουν οι προϋποθέσεις.
     */
    public AccessCard issueAccessCard(Date expirationDate) {
        if (this.accessCard != null) {
            throw new IllegalStateException("User already has an access card.");
        }

        // 1. Εύρεση του ενεργού και εγκεκριμένου αιτήματος
        RegistrationRequest activeApprovedRequest = this.registrationRequests.stream()
                .filter(r -> r.getStatus() == ActivityStatus.Active && r.isApproved())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot issue card: No ACTIVE and APPROVED registration request found."));

        // 2. Δημιουργία της κάρτας (ξεκινάει ως ACTIVE)
        AccessCard newCard = new AccessCard(expirationDate);

        // 3. Σύνδεση της κάρτας με τον User (Χρήση του Owning Side setter/helper)
        setAccessCard(newCard);

        // Το activeApprovedRequest παραμένει ACTIVE, εμποδίζοντας νέα submitRequests.

        return newCard;
    }

    // ------------------- Getters and Setters -------------------

    // Getters for simple fields
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    // Getters/Setters for Relationships (Helpers)
    public Set<RegistrationRequest> getRegistrationRequests() { return registrationRequests; }

    // Helper Method για αμφίδρομη συνοχή (RegistrationRequest)
    public void addRegistrationRequest(RegistrationRequest request) {
        this.registrationRequests.add(request);
        if (request.getUser() != this) {
            request.setUser(this);
        }
    }

    public AccessCard getAccessCard() { return accessCard; }

    // Helper Method για αμφίδρομη συνοχή (AccessCard)
    public void setAccessCard(AccessCard accessCard) {
        this.accessCard = accessCard;
        if (accessCard != null && accessCard.getUser() != this) {
            accessCard.setUser(this);
        }
    }

    // ------------------- Equals and HashCode -------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}