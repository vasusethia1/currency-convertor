package com.currency.converter.service;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.dto.ConversionRequestDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ExchangeRateService {


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
     * Fetches latest rates from the external API and saves all possible currency
     * combinations.
     * This method is scheduled to run daily.
     */
    void fetchAndSaveLatestRates();

}
