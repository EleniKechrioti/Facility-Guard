package org.aueb.representation.util;

public class ErrorResponse {
    public String message;
    public boolean success;

    public ErrorResponse(String message) {
        this.message = message;
        this.success = false;
    }
}