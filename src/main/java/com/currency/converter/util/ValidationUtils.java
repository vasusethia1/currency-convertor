package com.currency.converter.util;

import com.currency.converter.exception.DeprecatedCurrencyCodeException;
import com.currency.converter.exception.InvalidAmountException;
import com.currency.converter.exception.InvalidCurrencyException;
import com.currency.converter.exception.InvalidDateException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class ValidationUtils {

  private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");


  public static final Set<String> DEPRECATED_CURRENCY_CODES = Set.of(
    "DEM", "FRF", "ITL", "ESP", "NLG", "GRD", "PTE", "ATS", "BEF", "LUF", "IEP", "FIM",
    "SKK", "SIT", "CYP", "MTL", "TRL", "ZWD", "RUR", "YUM", "TMM"
  );

  public static final Map<String, String> DEPRECATED_TO_CURRENT_CURRENCY_MAP = Map.ofEntries(
    Map.entry("DEM", "EUR"),
    Map.entry("FRF", "EUR"),
    Map.entry("ITL", "EUR"),
    Map.entry("ESP", "EUR"),
    Map.entry("NLG", "EUR"),
    Map.entry("GRD", "EUR"),
    Map.entry("PTE", "EUR"),
    Map.entry("ATS", "EUR"),
    Map.entry("BEF", "EUR"),
    Map.entry("LUF", "EUR"),
    Map.entry("IEP", "EUR"),
    Map.entry("FIM", "EUR"),
    Map.entry("SKK", "EUR"),
    Map.entry("SIT", "EUR"),
    Map.entry("CYP", "EUR"),
    Map.entry("MTL", "EUR"),
    Map.entry("TRL", "TRY"), // Old Turkish Lira → New Turkish Lira
    Map.entry("ZWD", "ZWL"), // Zimbabwe Dollar → New Zimbabwe Dollar
    Map.entry("RUR", "RUB"), // Russian Ruble old → new
    Map.entry("YUM", "RSD"), // Yugoslav Dinar → Serbian Dinar
    Map.entry("TMM", "TMT")  // Turkmenistani Manat old → new
  );

  public static void validateCurrencyCode(String currency) {
    if (currency == null) {
      throw new InvalidCurrencyException("Currency code is null");
    }

    String upperCurrency = currency.toUpperCase();

    if (DEPRECATED_CURRENCY_CODES.contains(upperCurrency)) {
      String suggested = DEPRECATED_TO_CURRENT_CURRENCY_MAP.getOrDefault(upperCurrency, "N/A");
      throw new DeprecatedCurrencyCodeException(upperCurrency, suggested);
    }

    if (!CURRENCY_PATTERN.matcher(upperCurrency).matches()) {
      throw new InvalidCurrencyException(currency);
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
