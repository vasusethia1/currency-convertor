package com.currency.converter.strategy.impl;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.strategy.CurrencyConversionStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CrossCurrencyConversionStrategy implements CurrencyConversionStrategy {

    @Override
    public BigDecimal convert(ExchangeRateDTO sourceToBaseRate, ExchangeRateDTO targetToBaseRate, BigDecimal amount) {
        // Convert source currency to base currency (EUR)
        BigDecimal amountInBase = amount.divide(sourceToBaseRate.getRate(), 6, RoundingMode.HALF_UP);

        // Convert base currency to target currency
        return amountInBase.multiply(targetToBaseRate.getRate());
    }
}