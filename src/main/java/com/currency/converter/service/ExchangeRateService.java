package com.currency.converter.service;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.dto.ConversionRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateService {
    /**
     * Gets the exchange rate between two currencies for a specific date.
     * This method uses pre-calculated rates stored in the database.
     *
     * @param sourceCurrency The source currency code
     * @param targetCurrency The target currency code
     * @param date           The date for the exchange rate
     * @return ExchangeRateDTO containing the rate information
     */
    ExchangeRateDTO getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date);

    /**
     * Gets the exchange rate between two currencies for a specific date.
     * This method uses pre-calculated rates stored in the database.
     *
     * @param request The exchange rate request containing source and target
     *                currencies
     * @return ExchangeRateDTO containing the rate information
     */
    ExchangeRateDTO getExchangeRate(ExchangeRateRequestDTO request);

    /**
     * Converts an amount from source currency to target currency.
     * This method uses pre-calculated rates stored in the database.
     *
     * @param request The conversion request containing amount and currencies
     * @return The converted amount
     */
    BigDecimal convertCurrency(ConversionRequestDTO request);

    /**
     * Fetches latest rates from the external API and saves all possible currency
     * combinations.
     * This method is scheduled to run daily.
     */
    void fetchAndSaveLatestRates();

    /**
     * Fetches and stores exchange rates on application startup.
     */
    void fetchAndStoreExchangeRates();
}