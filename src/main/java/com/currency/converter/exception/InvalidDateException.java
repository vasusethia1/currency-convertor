package com.currency.converter.exception;

import org.springframework.http.HttpStatus;
import java.time.LocalDate;

public class InvalidDateException extends CurrencyConverterException {
    public InvalidDateException(LocalDate date) {
        super(
                "INVALID_DATE",
                "The provided date is not valid",
                String.format("Date '%s' cannot be in the future. " +
                        "Please provide a valid past or present date for historical rates.",
                        date),
                HttpStatus.BAD_REQUEST);
    }

    public InvalidDateException(String message) {
        super(
                "INVALID_DATE",
                "The provided date is not valid",
                message,
                HttpStatus.BAD_REQUEST);
    }
}