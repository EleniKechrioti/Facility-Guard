package org.aueb.representation;

import java.util.Date;

public class AlertRepresentation {
    public Integer id;
    public Date timestamp;
    public String message;

    // We only keep the ID of the Log that caused the Alert
    public Integer accessLogId;
}