package org.aueb.representation;

import org.aueb.util.enumerations.UserType;

public class UserRepresentation {
    public int id;          // Προσοχή: Στο Entity είναι userId, εδώ το λέμε απλά id για το JSON
    public String username;
    public String firstName;
    public String lastName;
    public String email;
    public UserType userType;

    // Δεν βάζουμε password εδώ για λόγους ασφαλείας.
    // Δεν βάζουμε AccessCard ή Requests για να αποφύγουμε infinite loops.
}