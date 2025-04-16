package com.currency.converter.exception;

public enum ErrorMessage {
    INVALID_CURRENCY(
            "INVALID_CURRENCY",
            "The currency code '%s' is not valid. Please provide a valid ISO 4217 currency code.",
            "Invalid currency code provided: %s"),
    INVALID_AMOUNT(
            "INVALID_AMOUNT",
            "The amount '%s' is not valid. Please provide a positive number.",
            "Invalid amount provided: %s"),
    EXCHANGE_RATE_NOT_FOUND(
            "EXCHANGE_RATE_NOT_FOUND",
            "Could not find exchange rate for currency pair %s/%s",
            "Exchange rate not found for currency pair: %s/%s"),
    EXTERNAL_SERVICE_ERROR(
            "EXTERNAL_SERVICE_ERROR",
            "An error occurred while fetching exchange rates. Please try again later.",
            "Error occurred while calling external exchange rate service: %s"),
    INTERNAL_SERVER_ERROR(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            "Internal server error: %s");

    private final String code;
    private final String userMessage;
    private final String logMessage;

    ErrorMessage(String code, String userMessage, String logMessage) {
        this.code = code;
        this.userMessage = userMessage;
        this.logMessage = logMessage;
    }

    public String getCode() {
        return code;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getLogMessage() {
        return logMessage;
    }
}