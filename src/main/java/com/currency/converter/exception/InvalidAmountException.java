package com.currency.converter.exception;

import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public class InvalidAmountException extends CurrencyConverterException {
    public InvalidAmountException(BigDecimal amount) {
        super(
                "INVALID_AMOUNT",
                "The conversion amount is not valid",
                String.format("Amount '%s' must be a positive number greater than zero. " +
                        "Please provide a valid amount for conversion.", amount),
                HttpStatus.BAD_REQUEST);
    }

    public InvalidAmountException(String message) {
        super(
                "INVALID_AMOUNT",
                "The conversion amount is not valid",
                message,
                HttpStatus.BAD_REQUEST);
    }
}