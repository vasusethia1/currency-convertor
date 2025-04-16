package com.currency.converter.exception;

import org.springframework.http.HttpStatus;

public class ExchangeRateApiException extends CurrencyConverterException {

    private final String apiName;
    private final String operation;

    public ExchangeRateApiException(String apiName, String operation, Throwable cause) {
        super(
                "EXCHANGE_RATE_API_ERROR",
                "The exchange rate service is temporarily unavailable. Please try again later.",
                String.format("Failed to %s from %s API. Error: %s", operation, apiName, cause.getMessage()),
                HttpStatus.SERVICE_UNAVAILABLE,
                cause);
        this.apiName = apiName;
        this.operation = operation;
    }

    public String getApiName() {
        return apiName;
    }

    public String getOperation() {
        return operation;
    }
}