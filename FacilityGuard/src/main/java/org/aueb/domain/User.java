package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.ActivityStatus;
import org.aueb.util.PasswordEncoder;
import org.aueb.util.enumerations.UserType;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

    /**
     * The unique identifier for the user.
     */
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int userId;

    /**
     * The unique username used for system login.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * The user's password.
     */
    @Column(nullable = false, length = 60)
    private String password;

    /** The user's first name. */
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    /** The user's last name. */
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    /** The user's email. */
    @Column(name = "email", length = 50, nullable = false)
    private String email;

    /**
     * The user's type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    /**
     * The user's access card
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "card_fk", referencedColumnName = "card_id")
    private AccessCard accessCard;

    /**
     * The set of all registration requests made by this user
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<RegistrationRequest> registrationRequests = new HashSet<>();

    /**
     * Default Constructor
     */
    public User() {}

    // Parameterized Constructor
    public User(String username, String password, String firstName, String lastName, String email, UserType userType){
        setUsername(username);
        setPassword(password);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setUserType(userType);
    }

    // ------------------- Business Methods -------------------

    /**
     * Creates and submits a new registration request
     * @throws IllegalStateException if the user already has an active request (status=ACTIVE).
     */
    public RegistrationRequest submitRegistrationRequest() {
        boolean hasActiveRequest = this.registrationRequests.stream()
                .anyMatch(r -> r.getStatus() == ActivityStatus.Active);

        if (hasActiveRequest) {
            throw new IllegalStateException("User " + this.username + " already has an active registration request. Cannot submit a new one.");
        }

        RegistrationRequest newRequest = new RegistrationRequest();

        addRegistrationRequest(newRequest);

        return newRequest;
    }

    /**
     * Creates a new AccessCard for the user, if there is an APPROVED request.
     * @param expirationDate the expiration date of the card.
     * @return the created access card.
     * @throws IllegalStateException if they don't fulfill the limitations.
     */
    public AccessCard issueAccessCard(Date expirationDate) {
        if (this.accessCard != null) {
            throw new IllegalStateException("User already has an access card.");
        }

        RegistrationRequest activeApprovedRequest = this.registrationRequests.stream()
                .filter(r -> r.getStatus() == ActivityStatus.Active && r.isApproved())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot issue card: No ACTIVE and APPROVED registration request found."));

        AccessCard newCard = new AccessCard(expirationDate);

        setAccessCard(newCard);
        return newCard;
    }

    public Set<RegistrationRequest> getRegistrationRequests() { return registrationRequests; }

    /**
     * Adds a RegistrationRequest to the collection
     * @param request The RegistrationRequest to add
     */
    public void addRegistrationRequest(RegistrationRequest request) {
        registrationRequests.add(request);
        request.setUser(this);
    }

    /**
     * Returns the access card of the user
     * @return the access card
     */
    public AccessCard getAccessCard() { return accessCard; }


    /**
     * Assigns an AccessCard to the user
     * If the user already has a card, it will be replaced.
     * @param accessCard The single AccessCard to assign.
     */
    public void setAccessCard(AccessCard accessCard) {
        this.accessCard = accessCard;
        if (accessCard != null && accessCard.getUser() != this) {
            accessCard.setUser(this);
        }
    }


    /**
     * Removes the currently assigned AccessCard from the user.
     */
    public void removeAccessCard() {
        if (this.accessCard != null) {
            this.accessCard.setUser(null);
            this.accessCard = null;
        }
    }



    /**
     * Checks if the user has Administrator privileges.
     * @return true if the userType is Administrator.
     */
    public boolean isAdmin() {
        return this.userType == UserType.Administrator;
    }

    /**
     * Checks if the user is an Employee.
     * @return true if the userType is Employee.
     */
    public boolean isEmployee() {
        return this.userType == UserType.Employee;
    }

    /**
     * Checks if the user is a visitor.
     * @return true if the userType is Visitor.
     */
    public boolean isVisitor() {
        return this.userType == UserType.Visitor;
    }


    //---------- Getters and Setters --------------------


    /**
     * Returns the user id
     * @return the user id
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Returns the username
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the stored password hash.
     * @return the stored password hash string
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = PasswordEncoder.encode(password);
    }

    /**
     * Checks if the provided password matches the stored hashed password.
     * This comparison is performed safely by the BCrypt algorithm.
     * @param password The clear-text password to check.
     * @return true if the passwords match, false otherwise.
     */
    public boolean checkPassword(String password) {
        if (this.password == null) {
            return false; // Cannot match against a null hash
        }
        return PasswordEncoder.matches(password, this.password);
    }

    /**
     * Returns the email
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email
     * @param email the email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns the first name
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name
     * @param firstName the first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the last name
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name
     * @param lastName the last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the User type
     * @return the user type
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Sets the User type
     * @param userType the user type
     */
    public void setUserType(UserType userType) {
        this.userType = userType;
    }


    /**
     * Equality depends on all fields of the address
     * @param o the other object
     * @return  {@code true} if the objects are equal
     */
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

    /**
     * Returns a string representation of the User object
     * @return a string representation of the object
     */
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", userType=" + userType +
                '}';
    }
}