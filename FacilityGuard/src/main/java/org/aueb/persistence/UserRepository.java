package org.aueb.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.aueb.domain.User;
import org.aueb.util.enumerations.UserType;

import java.util.List;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, Integer> {

    // Μπορούμε να προσθέσουμε custom queries εδώ
    public User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public List<User> findAdmins() {
        return find("userType", UserType.Administrator).list();
    }
}