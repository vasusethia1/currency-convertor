package com.currency.converter.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
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

    @Retryable(value = {
            RestClientException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 5000))
    public Map<String, BigDecimal> fetchLatestRates() {
        log.info("Fetching latest rates from external API");
        String url = String.format("%s?access_key=%s", apiUrl, apiKey);

        try {
            ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

            if (response == null) {
                log.error("Received null response from exchange rate API");
                throw new RestClientException("Null response received from API");
            }

            if (!response.isSuccess()) {
                log.error("API request was not successful");
                throw new RestClientException("API request failed");
            }

            log.info("Successfully fetched exchange rates");
            return response.getRates();
        } catch (RestClientException e) {
            log.error("Error fetching exchange rates: {}", e.getMessage());
            throw e;
        }
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