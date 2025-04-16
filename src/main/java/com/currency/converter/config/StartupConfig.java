package com.currency.converter.config;

import com.currency.converter.service.ExchangeRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupConfig {

    private final ExchangeRateService exchangeRateService;

    @Autowired
    public StartupConfig(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // Fetch and store exchange rates on startup
        exchangeRateService.fetchAndStoreExchangeRates();
    }
}