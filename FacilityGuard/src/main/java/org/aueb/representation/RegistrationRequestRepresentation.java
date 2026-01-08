package org.aueb.representation;

import org.aueb.util.enumerations.ActivityStatus;
import java.util.Date;

public class RegistrationRequestRepresentation {
    public int id;
    public Date requestDate;
    public boolean approved;
    public ActivityStatus status;
    public UserRepresentation user;
}