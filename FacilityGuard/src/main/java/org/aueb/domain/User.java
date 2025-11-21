package org.aueb.domain;

import jakarta.persistence.*;
import org.aueb.util.enumerations.UserType;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a system user (e.g., employee, administrator, visitor) who interacts with
 * the access control system.
 * This entity is mapped to the "USERS" table in the relational database.
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * The unique identifier for the user. Serves as the primary key.
     */
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int userId;

    /**
     * The unique username used for system login. Must be unique and non-null.
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * The user's password. Stored as a hash in a production environment. Non-null.
     */
    @Column(nullable = false)
    private String password;

    /**
     * The user's legal first name.
     */
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    /**
     * The user's legal last name.
     */
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    /**
     * The user's email address
     */
    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING) // Αποθηκεύει το όνομα του Enum (π.χ. "Administrator")
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    /**
     * Default Constructor
     */
    public User() {
    }

    /**
     * Constructor
     *
     * @param username The username of the User
     * @param password The password of the User
     * @param firstName The first name of the User
     * @param lastName The last name of the User
     */
    public User(String username, String password, String firstName, String lastName, String email, UserType userType){
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userType = userType;
    }

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
     * Returns the password
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
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

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;                      }
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
}
