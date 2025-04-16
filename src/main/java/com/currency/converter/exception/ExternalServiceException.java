package com.currency.converter.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends CurrencyConverterException {
    public ExternalServiceException(String serviceName, String operation, String errorMessage) {
        super(
                "EXTERNAL_SERVICE_ERROR",
                String.format("Error while accessing %s service", serviceName),
                String.format("Failed to %s from %s service. Error: %s. " +
                        "Please try again later or contact support if the issue persists.",
                        operation, serviceName, errorMessage),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ExternalServiceException(String serviceName, String operation, Throwable cause) {
        super(
                "EXTERNAL_SERVICE_ERROR",
                String.format("Error while accessing %s service", serviceName),
                String.format("Failed to %s from %s service. Error: %s",
                        operation, serviceName, cause.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
    }
}