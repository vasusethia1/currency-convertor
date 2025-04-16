package com.currency.converter.exception;


public class StaleExchangeRateDataException extends RuntimeException {

  public StaleExchangeRateDataException(String message) {
    super(message);
  }

  public StaleExchangeRateDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
