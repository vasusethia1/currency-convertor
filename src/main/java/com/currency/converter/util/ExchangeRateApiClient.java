package com.currency.converter.util;

import com.currency.converter.exception.ExchangeRateApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

  private final RestTemplate restTemplate;

  @Value("${exchange.rate.api.url}")
  private String apiUrl;

  @Value("${exchange.rate.api.url.historic}")
  private String historyApiUrl;

  @Value("${exchange.rate.api.key}")
  private String apiKey;

  @Retry(name = "exchangeRateApiRetry", fallbackMethod = "fallbackFetchLatestRates")
  @CircuitBreaker(name = "exchangeRateApiCircuitBreaker", fallbackMethod = "fallbackFetchLatestRates")
  public Map<String, BigDecimal> fetchLatestRates() {
    log.info("Fetching latest rates from external API");
    String url = String.format("%s?access_key=%s", apiUrl, apiKey);

    ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

    if (response == null || !response.isSuccess()) {
      log.error("Failed to fetch exchange rates from API");
      throw new RestClientException("Exchange rate API failure");
    }

    return response.getRates();
  }


  public Map<String, BigDecimal> fetchRealTimeBaseRates(String fromCurrency, LocalDateTime dateTime) {
    log.info("Fetching exchange rates for base: {} on {}", fromCurrency, dateTime.toLocalDate());

    String url;

    if (dateTime.toLocalDate().isEqual(LocalDate.now())) {
      url = String.format("%s?access_key=%s&base=%s", apiUrl, apiKey, fromCurrency);
    } else {
      String datePart = dateTime.toLocalDate().toString();
      fromCurrency = "EUR";
      url = String.format("%s/%s?access_key=%s&base=%s", historyApiUrl, datePart, apiKey, fromCurrency);
    }

    log.info("Fetching response from URL: {}", url);

    ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

    if (response == null || !response.isSuccess()) {
      log.error("Failed to fetch exchange rates from API for base {} on {}", fromCurrency, dateTime.toString());
      throw new RestClientException("Exchange rate API failure");
    }
    log.info("Processing response from the api");
    if (dateTime.toLocalDate().isEqual(LocalDate.now())) {
      long apiTimestamp = response.getTimestamp();
      Instant apiInstant = Instant.ofEpochSecond(apiTimestamp);
      Instant now = Instant.now();
      long hoursDifference = ChronoUnit.HOURS.between(apiInstant, now);

      if (hoursDifference > 1) {
        log.warn("Exchange rates for {} on {} are outdated by {} hours. Re-fetching may be required.",
          fromCurrency, dateTime.toLocalDate(), hoursDifference);
      }
    }

    return response.getRates();
  }


  public Map<String, BigDecimal> fallbackFetchLatestRates(Throwable t) {
    log.error("Fallback triggered for fetchLatestRates: {}", t.getMessage());
    throw new ExchangeRateApiException("Exchange Rate API", "fallback after retries/circuit breaker", t);
  }


  @Getter
  private static class ExchangeRateResponse {
    private boolean success;
    private Map<String, BigDecimal> rates;
    private Long timestamp;

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public void setRates(Map<String, BigDecimal> rates) {
      this.rates = rates;
    }
  }
}
