package com.currency.converter.service.impl;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.dto.ConversionRequestDTO;
import com.currency.converter.entity.ExchangeRate;
import com.currency.converter.entity.ExchangeRateMetadata;
import com.currency.converter.exception.*;
import com.currency.converter.repository.ExchangeRateMetadataRepository;
import com.currency.converter.repository.ExchangeRateRepository;
import com.currency.converter.service.ExchangeRateService;
import com.currency.converter.strategy.CurrencyConversionStrategy;
import com.currency.converter.util.ExchangeRateApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

  private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
  private static final long STALE_THRESHOLD_HOURS = 24;
  private static final String LOCK_KEY = "fetchRatesLock";
  private static final String EXCHANGE_RATE_FRESH_KEY = "exchange-rate:fresh";
  private static final long FRESHNESS_TTL_HOURS = 24;


  private final ExchangeRateRepository exchangeRateRepository;
  private final ExchangeRateApiClient exchangeRateApiClient;
  private final ExchangeRateMetadataRepository exchangeRateMetadataRepository;
  private final RedissonClient redissonClient;
  private final RedisTemplate<String, Boolean> redisTemplate;

  @Value("${exchange.rate.api.base-currency}")
  private String baseCurrency;

  @Override
  @Cacheable(value = "exchangeRates", key = "#sourceCurrency + '-' + #targetCurrency + '-' + #date")
  public ExchangeRateDTO getExchangeRate(String sourceCurrency, String targetCurrency, LocalDate date) {
    log.info("Fetching exchange rate for {} to {} on {}", sourceCurrency, targetCurrency, date);
    checkDataFreshness();

    validateCurrencyCode(sourceCurrency);
    validateCurrencyCode(targetCurrency);
    validateDate(date);

    try {
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

      RLock lock = redissonClient.getLock(LOCK_KEY);
      if (lock.isLocked() && date.isEqual(LocalDate.now())) {
        log.info("Scheduler is running and date is today, fetching real-time rate from API for {} to {} on {}",
          request.getSourceCurrency(), request.getTargetCurrency(), date);
        try {
          var rateDto =  exchangeRateApiClient.fetchRealTimeBaseRates(
            request.getSourceCurrency(), request.getTargetCurrency());

          if (rateDto == null) {
            throw new ExchangeRateApiException("Exchange Rate API",
              "fetch real-time rates for " + request.getSourceCurrency() + " to " + request.getTargetCurrency(),
              new RuntimeException("Invalid rate returned from API"));
          }

          ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO();
          exchangeRateDTO.setDate(request.getDate());
          exchangeRateDTO.setTimestamp(System.currentTimeMillis());
          exchangeRateDTO.setBaseCurrency(request.getSourceCurrency());
          exchangeRateDTO.setTargetCurrency(request.getTargetCurrency());
          exchangeRateDTO.setRate(rateDto.get(request.getTargetCurrency()));

          return exchangeRateDTO;

        } catch (Exception ex) {
          log.error("Error fetching real-time rate from API: {}", ex.getMessage());
          // Fallback to database on API failure
          log.info("Falling back to database due to API failure");
        }
      }
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
  @CacheEvict(value = "exchangeRates", allEntries = true)
  @Transactional
  public void fetchAndSaveLatestRates() {
    log.info("Starting to fetch and save latest exchange rates");

    RLock lock = redissonClient.getLock(LOCK_KEY);
    LocalDate today = LocalDate.now();
    try {
      if (lock.tryLock(0, 10, TimeUnit.SECONDS)) {
        log.info("Acquired lock, fetching exchange rates");

        Map<String, BigDecimal> rates = exchangeRateApiClient.fetchLatestRates();
        if (rates == null || rates.isEmpty()) {
          throw new ExchangeRateApiException("Exchange Rate API", "fetch latest rates",
            new RuntimeException("No rates returned from API"));
        }

        rates.put(baseCurrency, BigDecimal.ONE);

        // Generate all possible currency pairs
        List<ExchangeRate> allRates = new ArrayList<>();
        List<String> currencies = new ArrayList<>(rates.keySet());

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

        exchangeRateMetadataRepository.save(ExchangeRateMetadata.builder()
          .lastSuccessfulSyncTime(LocalDateTime.now(ZoneOffset.UTC))
          .syncStatus("SUCCESS")
          .source("ExchangeRateAPI")
          .notes("Fetched and saved successfully.")
          .build());
        log.info("Successfully saved {} exchange rates for {}", allRates.size(), today);

      } else {
        log.info("Lock not acquired, another instance is fetching the rates.");
      }
    } catch (InterruptedException e) {
      log.error("Failed to acquire lock", e);
    } catch (Exception ex) {
      log.error("Error fetching and saving latest rates: {}", ex.getMessage());
      exchangeRateMetadataRepository.save(ExchangeRateMetadata.builder()
        .lastSuccessfulSyncTime(LocalDateTime.now(ZoneOffset.UTC))
        .syncStatus("FAILURE")
        .source("ExchangeRateAPI")
        .notes(ex.getMessage())
        .build());
      throw new ExchangeRateApiException("Exchange Rate API", "fetch and save latest rates", ex);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
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
      return BigDecimal.ONE.divide(rates.get(fromCurrency), 6, RoundingMode.HALF_UP);
    } else {
      BigDecimal fromToBase = rates.get(fromCurrency);
      BigDecimal toToBase = rates.get(toCurrency);
      return toToBase.divide(fromToBase, 6, RoundingMode.HALF_UP);
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

  private void checkDataFreshness() {
    Boolean isFresh = redisTemplate.opsForValue().get(EXCHANGE_RATE_FRESH_KEY);
    if (Boolean.TRUE.equals(isFresh)) {
      return; // Data is fresh, skip DB check
    }

    log.debug("Freshness not found in Redis, checking DB metadata...");

    ExchangeRateMetadata metadata = exchangeRateMetadataRepository.findTopByOrderByLastSuccessfulSyncTimeDesc();
    if (metadata == null || metadata.getLastSuccessfulSyncTime() == null) {
      throw new StaleExchangeRateDataException("Exchange rate data has never been synced.");
    }

    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    if (metadata.getLastSuccessfulSyncTime().isBefore(now.minusHours(STALE_THRESHOLD_HOURS))) {
      throw new StaleExchangeRateDataException("Exchange rate data is stale. Last sync was at " +
        metadata.getLastSuccessfulSyncTime());
    }

    // Data is fresh - cache that info
    redisTemplate.opsForValue().set(EXCHANGE_RATE_FRESH_KEY, true, FRESHNESS_TTL_HOURS, TimeUnit.HOURS);
    log.debug("Exchange rate freshness marked as true in Redis");
  }


}
