package com.currency.converter.exception;

public class DeprecatedCurrencyCodeException extends RuntimeException {
  public DeprecatedCurrencyCodeException(String code, String suggestedCode) {
    super("Currency code '" + code + "' is deprecated and no longer supported."
      + (suggestedCode != null ? " Please use '" + suggestedCode + "' instead." : ""));
  }
}
