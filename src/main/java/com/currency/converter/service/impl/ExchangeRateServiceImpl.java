package com.currency.converter.service.impl;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.entity.ExchangeRate;
import com.currency.converter.entity.ExchangeRateMetadata;
import com.currency.converter.exception.*;
import com.currency.converter.repository.ExchangeRateMetadataRepository;
import com.currency.converter.repository.ExchangeRateRepository;
import com.currency.converter.service.ExchangeRateService;
import com.currency.converter.util.ExchangeRateApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static com.currency.converter.util.ValidationUtils.validateCurrencyCode;
import static com.currency.converter.util.ValidationUtils.validateDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

  private static final String LOCK_KEY = "fetchRatesLock";
  private static final String EXCHANGE_RATE_FRESH_KEY = "exchange-rate:fresh";
  private static final long FRESHNESS_TTL_HOURS = 24;


  private final ExchangeRateRepository exchangeRateRepository;
  private final ExchangeRateApiClient exchangeRateApiClient;
  private final ExchangeRateMetadataRepository exchangeRateMetadataRepository;
  private final RedissonClient redissonClient;
  private final ObjectMapper objectMapper;

  @Autowired
  @Qualifier("exchangeRateRedisTemplate")
  private RedisTemplate<String, ExchangeRateDTO> exchangeRateRedisTemplate;

  private final RedisTemplate<String, Boolean> redisTemplate;

  @Value("${exchange.rate.api.base-currency}")
  private String baseCurrency;


  @Override
  public ExchangeRateDTO getExchangeRate(ExchangeRateRequestDTO request) {
    log.info("Fetching exchange rate for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(), request.getDate());


    try {

      validateCurrencyCode(request.getSourceCurrency());
      validateCurrencyCode(request.getTargetCurrency());
      LocalDateTime date = request.getDate() != null ? request.getDate() : LocalDateTime.now().toLocalDate().atStartOfDay();
      validateDate(date);
      String cacheKey = request.getSourceCurrency() + "-" + request.getTargetCurrency() + "-" + date;


      Object cachedValue = exchangeRateRedisTemplate.opsForValue().get(cacheKey);
      ExchangeRateDTO cachedRate = objectMapper.convertValue(cachedValue, ExchangeRateDTO.class);


      if (cachedRate != null) {
        log.info("Returning exchange rate from cache for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(), date);
        return cachedRate;
      }

      log.info("Cache miss, fetching exchange rate from database for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(), date);

      var response = fetchFromDb(request, date);

      if(response.isEmpty()){
        log.info("Directly fetching the rates from the external api");
        var apiResponse = exchangeRateApiClient.fetchRealTimeBaseRates(request.getSourceCurrency(), date);

        var exchangeRate = storeRatesToDbAndReturnExpectedExchangeRate(apiResponse, date, request.getSourceCurrency(), request.getTargetCurrency());

        if(exchangeRate.isEmpty()){
          throw new RuntimeException("Exception occurred while fetching from the api");
        }
        ExchangeRateDTO exchangeRateDTO = convertToDTO(exchangeRate.get());
        exchangeRateRedisTemplate.opsForValue().set(cacheKey, exchangeRateDTO, 1, TimeUnit.HOURS);

        log.info("Cached exchange rate for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(), date);

        return exchangeRateDTO;
      }

      log.info("Cached exchange rate for {} to {} on {}", request.getSourceCurrency(), request.getTargetCurrency(), date);

      ExchangeRateDTO exchangeRateDTO = convertToDTO(response.get());
      exchangeRateRedisTemplate.opsForValue().set(cacheKey, exchangeRateDTO, 1, TimeUnit.HOURS);
      return exchangeRateDTO;

    } catch (ExchangeRateNotFoundException | InvalidCurrencyException | InvalidDateException ex) {
      log.info("Error retrieving exchange rate: {}", ex.getMessage());

      throw ex;
    } catch (Exception ex) {
      log.error("Error retrieving exchange rate: {}", ex.getMessage());
      throw new DatabaseException("retrieve", "exchange rate", ex);
    }
  }

  public Optional<ExchangeRate> fetchFromDb(ExchangeRateRequestDTO request, LocalDateTime date){
    Optional<ExchangeRate> rate = exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndDate(
      request.getSourceCurrency(), request.getTargetCurrency(), date);

    if (rate.isEmpty()) {
      log.info("No rate found for exact date {}, looking for latest rate before this date", date);
      return  Optional.empty();
    }
    return rate;
  }


  @Override
  @Scheduled(cron = "0 0 0 * * ?", zone = "UTC")
  @CacheEvict(value = "exchangeRates", allEntries = true)
  @Transactional
  public void fetchAndSaveLatestRates() {
    LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
    RLock lock = null;
    boolean lockAcquired = false;

    try {
      try {
        lock = redissonClient.getLock(LOCK_KEY);
        lockAcquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
      } catch (Exception e) {
        log.warn("Redis is down or Redisson lock failed. Proceeding without distributed lock: {}", e.getMessage());
      }

      if (lockAcquired || lock == null) {
        log.info("Proceeding to fetch exchange rates{}", lockAcquired ? " with lock" : " without lock");

        Map<String, BigDecimal> rates = exchangeRateApiClient.fetchLatestRates();
        storeRatesToDbAndReturnExpectedExchangeRate(rates, today);

        exchangeRateMetadataRepository.save(ExchangeRateMetadata.builder()
          .lastSuccessfulSyncTime(LocalDateTime.now(ZoneOffset.UTC))
          .syncStatus("SUCCESS")
          .source("ExchangeRateAPI")
          .notes("Fetched and saved successfully.")
          .build());

          redisTemplate.opsForValue().set(EXCHANGE_RATE_FRESH_KEY, true, FRESHNESS_TTL_HOURS, TimeUnit.HOURS);

          log.info("Exchange rates updated successfully for {}", today);
      } else {
        log.info("Lock not acquired. Another instance might be fetching the rates.");
      }
    } catch (Exception ex) {
      log.error("Error fetching and saving latest rates: {}", ex.getMessage(), ex);
      exchangeRateMetadataRepository.save(ExchangeRateMetadata.builder()
        .lastSuccessfulSyncTime(LocalDateTime.now(ZoneOffset.UTC))
        .syncStatus("FAILURE")
        .source("ExchangeRateAPI")
        .notes(ex.getMessage())
        .build());

      throw new ExchangeRateApiException("Exchange Rate API", "fetch and save latest rates", ex);
    } finally {
      try {
        if (lockAcquired && lock != null && lock.isHeldByCurrentThread()) {
          lock.unlock();
        }
      } catch (Exception e) {
        log.warn("Failed to release Redis lock: {}", e.getMessage());
      }
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

  private ExchangeRate createExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDateTime date) {
    return ExchangeRate.builder()
      .baseCurrency(fromCurrency)
      .targetCurrency(toCurrency)
      .rate(rate)
      .date(date)
      .timestamp(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
      .build();
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

  public Optional<ExchangeRate> storeRatesToDbAndReturnExpectedExchangeRate(Map<String, BigDecimal> rates, LocalDateTime date){
    return storeRatesToDbAndReturnExpectedExchangeRate(rates, date, "NA", "NA");
  }
  public Optional<ExchangeRate> storeRatesToDbAndReturnExpectedExchangeRate(
    Map<String, BigDecimal> rates, LocalDateTime date, String inputCurrency, String outputCurrency) {

    if (rates == null || rates.isEmpty()) {
      throw new ExchangeRateApiException("Exchange Rate API", "fetch latest rates",
        new RuntimeException("No exchange rates returned from the API"));
    }

    BigDecimal expectedRate = calculateRate(rates, inputCurrency, outputCurrency);

    CompletableFuture.runAsync(() -> storeRatesToDbAsync(rates, date));

    return Optional.ofNullable(createExchangeRate(inputCurrency, outputCurrency, expectedRate, date));
  }

  @Async
  public void storeRatesToDbAsync(Map<String, BigDecimal> rates, LocalDateTime date) {
    List<ExchangeRate> allRates = new ArrayList<>();
    List<String> currencies = new ArrayList<>(rates.keySet());

    for (String fromCurrency : currencies) {
      for (String toCurrency : currencies) {
        if (!fromCurrency.equals(toCurrency)) {
          BigDecimal rate = calculateRate(rates, fromCurrency, toCurrency);
          ExchangeRate exchangeRate = createExchangeRate(fromCurrency, toCurrency, rate, date);
          allRates.add(exchangeRate);
        }
      }
    }

    exchangeRateRepository.saveAll(allRates);
  }

}
