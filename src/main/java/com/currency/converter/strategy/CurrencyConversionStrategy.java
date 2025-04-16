package com.currency.converter.strategy;

import com.currency.converter.dto.ExchangeRateDTO;
import java.math.BigDecimal;

public interface CurrencyConversionStrategy {
    BigDecimal convert(ExchangeRateDTO sourceToBaseRate, ExchangeRateDTO targetToBaseRate, BigDecimal amount);
}