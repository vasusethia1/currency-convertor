package com.currency.converter.exception;

import org.springframework.http.HttpStatus;

public class DatabaseException extends CurrencyConverterException {
    public DatabaseException(String operation, String entity, String errorMessage) {
        super(
                "RESOURCE_ERROR",
                String.format("Error while performing database operation on %s", entity),
                String.format("Failed to %s %s. Error: %s. " +
                        "Please try again later or contact support if the issue persists.",
                        operation, entity, errorMessage),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public DatabaseException(String operation, String entity, Throwable cause) {
        super(
                "RESOURCE_ERROR",
                String.format("Error while performing database operation on %s", entity),
                String.format("Failed to %s %s. Error: %s",
                        operation, entity, cause.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR,
                cause);
    }
}