package com.currency.converter.service.impl;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.dto.ConversionRequestDTO;
import com.currency.converter.entity.ExchangeRate;
import com.currency.converter.exception.*;
import com.currency.converter.repository.ExchangeRateRepository;
import com.currency.converter.service.ExchangeRateService;
import com.currency.converter.strategy.CurrencyConversionStrategy;
import com.currency.converter.util.ExchangeRateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final CurrencyConversionStrategy currencyConversionStrategy;

    @Value("${exchange.rate.api.base-currency}")
    private String baseCurrency;

    @Override
    @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '-' + #targetCurrency + '-' + #date")
    public ExchangeRateDTO getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date) {
        log.info("Fetching exchange rate for {} to {} on {}", sourceCurrency, targetCurrency, date);

        validateCurrencyCode(sourceCurrency);
        validateCurrencyCode(targetCurrency);
        validateDate(date);

        try {
            // Try to get the rate directly from the database
            Optional<ExchangeRate> rate = exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate(
                    sourceCurrency, targetCurrency, date);

            if (rate.isEmpty()) {
                log.info("No rate found for exact date {}, looking for latest rate before this date", date);
                rate = exchangeRateRepository.findLatestRateBeforeDate(sourceCurrency, targetCurrency, date);
            }

            return rate.map(this::convertToDTO)
                    .orElseThrow(
                            () -> new ExchangeRateNotFoundException(sourceCurrency, targetCurrency, date.toString()));
        } catch (Exception ex) {
            log.error("Error retrieving exchange rate: {}", ex.getMessage());
            throw new DatabaseException("retrieve", "exchange rate", ex);
        }
    }

    @Override
    @Cacheable(value = "exchangeRates", key = "#request.sourceCurrency + '-' + #request.targetCurrency + '-' + #request.date")
    public ExchangeRateDTO getExchangeRate(ExchangeRateRequestDTO request) {
        log.info("Fetching exchange rate for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(),
                request.getDate());

        validateCurrencyCode(request.getSourceCurrency());
        validateCurrencyCode(request.getTargetCurrency());
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        validateDate(date);

        try {
            // Try to get the rate directly from the database
            Optional<ExchangeRate> rate = exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate(
                    request.getSourceCurrency(), request.getTargetCurrency(), date);

            if (rate.isEmpty()) {
                log.info("No rate found for exact date {}, looking for latest rate before this date", date);
                rate = exchangeRateRepository.findLatestRateBeforeDate(
                        request.getSourceCurrency(), request.getTargetCurrency(), date);
            }

            if (rate.isEmpty()) {
                log.info("No rate found for {} to {} on or before {}",
                        request.getSourceCurrency(), request.getTargetCurrency(), date);
                throw new ExchangeRateNotFoundException(
                        request.getSourceCurrency(),
                        request.getTargetCurrency(),
                        date.toString());
            }

            return convertToDTO(rate.get());
        } catch (ExchangeRateNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error retrieving exchange rate: {}", ex.getMessage());
            throw new DatabaseException("retrieve", "exchange rate", ex);
        }
    }

    @Override
    @Transactional
    public BigDecimal convertCurrency(ConversionRequestDTO request) {
        log.info("Converting {} from {} to {}", request.getAmount(), request.getSourceCurrency(),
                request.getTargetCurrency());

        validateCurrencyCode(request.getSourceCurrency());
        validateCurrencyCode(request.getTargetCurrency());
        validateAmount(request.getAmount());
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        validateDate(date);

        try {
            // Get the direct exchange rate
            ExchangeRateDTO rate = getExchangeRate(request.getSourceCurrency(), request.getTargetCurrency(), date);

            // Perform the conversion
            return request.getAmount().multiply(rate.getRate());
        } catch (Exception ex) {
            log.error("Error during currency conversion: {}", ex.getMessage());
            throw new DatabaseException("perform", "currency conversion", ex);
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
    @Transactional
    public void fetchAndSaveLatestRates() {
        log.info("Starting to fetch and save latest exchange rates");
        LocalDate today = LocalDate.now();

        try {
            // Fetch latest rates from API
            Map<String, BigDecimal> rates = exchangeRateApiClient.fetchLatestRates();
            if (rates == null || rates.isEmpty()) {
                throw new ExchangeRateApiException("Exchange Rate API", "fetch latest rates",
                        new RuntimeException("No rates returned from API"));
            }

            // Add base currency rate (1.0)
            rates.put(baseCurrency, BigDecimal.ONE);

            // Generate all possible currency pairs
            List<ExchangeRate> allRates = new ArrayList<>();
            List<String> currencies = new ArrayList<>(rates.keySet());

            // Generate direct rates (e.g., EUR to USD)
            for (String fromCurrency : currencies) {
                for (String toCurrency : currencies) {
                    if (!fromCurrency.equals(toCurrency)) {
                        BigDecimal rate = calculateRate(rates, fromCurrency, toCurrency);
                        allRates.add(createExchangeRate(fromCurrency, toCurrency, rate, today));
                    }
                }
            }

            // Save all rates in a single transaction
            exchangeRateRepository.saveAll(allRates);
            log.info("Successfully saved {} exchange rates for {}", allRates.size(), today);

        } catch (Exception ex) {
            log.error("Error fetching and saving latest rates: {}", ex.getMessage());
            throw new ExchangeRateApiException("Exchange Rate API", "fetch and save latest rates", ex);
        }
    }

    @Override
    @Transactional
    public void fetchAndStoreExchangeRates() {
        log.info("Fetching and storing exchange rates on startup");
        try {
            fetchAndSaveLatestRates();
            log.info("Successfully fetched and stored exchange rates on startup");
        } catch (Exception e) {
            log.error("Error fetching and storing exchange rates on startup: {}", e.getMessage(), e);
            throw new ExchangeRateApiException("Exchange Rate API", "fetch rates", e);
        }
    }

    private BigDecimal calculateRate(Map<String, BigDecimal> rates, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(baseCurrency)) {
            return rates.get(toCurrency);
        } else if (toCurrency.equals(baseCurrency)) {
            return BigDecimal.ONE.divide(rates.get(fromCurrency), 6, BigDecimal.ROUND_HALF_UP);
        } else {
            // For non-base to non-base conversion (e.g., USD to INR)
            BigDecimal fromToBase = rates.get(fromCurrency);
            BigDecimal toToBase = rates.get(toCurrency);
            return toToBase.divide(fromToBase, 6, BigDecimal.ROUND_HALF_UP);
        }
    }

    private ExchangeRate createExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate date) {
        return ExchangeRate.builder()
                .baseCurrency(fromCurrency)
                .targetCurrency(toCurrency)
                .rate(rate)
                .date(date)
                .timestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .build();
    }

    private void validateCurrencyCode(String currency) {
        if (currency == null || !CURRENCY_PATTERN.matcher(currency).matches()) {
            throw new InvalidCurrencyException(currency);
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(amount);
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new InvalidDateException("Date cannot be null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidDateException("Date cannot be in the future");
        }
    }

    private ExchangeRateDTO convertToDTO(ExchangeRate rate) {
        return ExchangeRateDTO.builder()
                .baseCurrency(rate.getBaseCurrency())
                .targetCurrency(rate.getTargetCurrency())
                .rate(rate.getRate())
                .date(rate.getDate())
                .timestamp(rate.getTimestamp())
                .build();
    }
}