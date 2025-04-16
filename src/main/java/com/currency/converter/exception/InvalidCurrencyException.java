package com.currency.converter.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrencyException extends CurrencyConverterException {
    private final String currencyCode;

    public InvalidCurrencyException(String currencyCode) {
        super(
                ErrorMessage.INVALID_CURRENCY.getCode(),
                String.format(ErrorMessage.INVALID_CURRENCY.getUserMessage(), currencyCode),
                String.format(ErrorMessage.INVALID_CURRENCY.getLogMessage(), currencyCode),
                HttpStatus.BAD_REQUEST);
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}