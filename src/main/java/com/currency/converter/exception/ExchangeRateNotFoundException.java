package com.currency.converter.exception;

import org.springframework.http.HttpStatus;

public class ExchangeRateNotFoundException extends CurrencyConverterException {
        public ExchangeRateNotFoundException(String baseCurrency, String targetCurrency, String date) {
                super(
                                "EXCHANGE_RATE_NOT_FOUND",
                                String.format("Unable to find exchange rate for conversion from %s to %s", baseCurrency,
                                                targetCurrency),
                                String.format("No exchange rate data available for the specified currencies (%s to %s) on date %s. "
                                                +
                                                "Please check if the currencies are valid or try a different date.",
                                                baseCurrency, targetCurrency, date),
                                HttpStatus.NOT_FOUND);
        }

        public ExchangeRateNotFoundException(String baseCurrency, String targetCurrency, String date, Throwable cause) {
                super(
                                "EXCHANGE_RATE_NOT_FOUND",
                                String.format("Failed to retrieve exchange rate for %s to %s", baseCurrency,
                                                targetCurrency),
                                String.format("No exchange rate data available for the specified currencies (%s to %s) on date %s. Error: %s",
                                                baseCurrency, targetCurrency, date, cause.getMessage()),
                                HttpStatus.NOT_FOUND,
                                cause);
        }
}