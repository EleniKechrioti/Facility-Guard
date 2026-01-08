package org.aueb.representation;

import org.aueb.util.enumerations.ActivityStatus;
import java.util.Date;

public class AccessCardRepresentation {
    public int id;
    public Date expirationDate;
    public ActivityStatus status;
    public UserRepresentation holder;
}