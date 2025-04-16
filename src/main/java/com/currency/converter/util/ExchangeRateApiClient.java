package com.currency.converter.util;

import com.currency.converter.exception.ExchangeRateApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

  private final RestTemplate restTemplate;

  @Value("${exchange.rate.api.url}")
  private String apiUrl;

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


  public Map<String, BigDecimal> fetchRealTimeBaseRates(String fromCurrency, String toCurrency) {
    log.info("Fetching latest rates from external API");
    String url = String.format("%s?access_key=%s&base=%s&symbol%s", apiUrl, apiKey, fromCurrency, toCurrency);

    ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

    if (response == null || !response.isSuccess()) {
      log.error("Failed to fetch exchange rates from API");
      throw new RestClientException("Exchange rate API failure");
    }

    return response.getRates();
  }

  public Map<String, BigDecimal> fallbackFetchLatestRates(Throwable t) {
    log.error("Fallback triggered for fetchLatestRates: {}", t.getMessage());
    throw new ExchangeRateApiException("Exchange Rate API", "fallback after retries/circuit breaker", t);
  }


  private static class ExchangeRateResponse {
    private boolean success;
    private Map<String, BigDecimal> rates;

    public boolean isSuccess() {
      return success;
    }

    public void setSuccess(boolean success) {
      this.success = success;
    }

    public Map<String, BigDecimal> getRates() {
      return rates;
    }

    public void setRates(Map<String, BigDecimal> rates) {
      this.rates = rates;
    }
  }
}
