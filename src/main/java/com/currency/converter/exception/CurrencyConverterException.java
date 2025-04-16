package com.currency.converter.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CurrencyConverterException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;
    private final String detailedMessage;
    private final HttpStatus httpStatus;

    public CurrencyConverterException(String errorCode, String userMessage, String detailedMessage,
            HttpStatus httpStatus) {
        super(detailedMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.detailedMessage = detailedMessage;
        this.httpStatus = httpStatus;
    }

    public CurrencyConverterException(String errorCode, String userMessage, String detailedMessage,
            HttpStatus httpStatus, Throwable cause) {
        super(detailedMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.detailedMessage = detailedMessage;
        this.httpStatus = httpStatus;
    }
}