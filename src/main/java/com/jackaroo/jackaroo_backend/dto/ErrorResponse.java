package com.jackaroo.jackaroo_backend.dto;

public class ErrorResponse {
    private String message;
    private String type;

    public ErrorResponse(String message, String type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() { return message; }
    public String getType() { return type; }
}
