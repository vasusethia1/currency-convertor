package com.currency.converter.util;

import com.currency.converter.exception.InvalidAmountException;
import com.currency.converter.exception.InvalidCurrencyException;
import com.currency.converter.exception.InvalidDateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;


public class ValidationUtils {

  private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

  public static void validateCurrencyCode(String currency) {
    if (currency == null || !CURRENCY_PATTERN.matcher(currency).matches()) {
      throw new InvalidCurrencyException(currency);
    }
  }

  private static void validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidAmountException(amount);
    }
  }

  public static void validateDate(LocalDateTime date) {
    if (date == null) {
      throw new InvalidDateException("Date cannot be null");
    }
    if (date.isAfter(LocalDateTime.now().toLocalDate().atStartOfDay())) {
      throw new InvalidDateException("Date cannot be in the future");
    }
  }
}
