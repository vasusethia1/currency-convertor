package com.zeref.currencyconvertor.integration;

import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.entity.ExchangeRate;
import com.currency.converter.repository.ExchangeRateRepository;
import com.currency.converter.service.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ContextConfiguration(initializers = ExchangeRateServiceImplTest.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = com.currency.converter.CurrencyConverterApplication.class)
class ExchangeRateServiceImplTest {

  @Mock
  ObjectMapper objectMapper;

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4")
    .withExposedPorts(6379);

  static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
      TestPropertyValues.of(
        "spring.datasource.url=" + postgres.getJdbcUrl(),
        "spring.datasource.username=" + postgres.getUsername(),
        "spring.datasource.password=" + postgres.getPassword(),
        "spring.redis.host=" + redis.getHost(),
        "spring.redis.port=" + redis.getMappedPort(6379)
      ).applyTo(context.getEnvironment());
    }
  }

  @Autowired
  private ExchangeRateService exchangeRateService;

  @Autowired
  private ExchangeRateRepository exchangeRateRepository;

  @Autowired
  private RedisTemplate<String, ExchangeRateDTO> exchangeRateRedisTemplate;

  @Test
  @Order(1)
  void getExchangeRate_shouldReturnRateFromApi() {
    LocalDateTime date = LocalDateTime.of(2025, 4, 18, 0, 0);
    ExchangeRateRequestDTO request = new ExchangeRateRequestDTO("EUR", "INR", date);

    ExchangeRateDTO result = exchangeRateService.getExchangeRate(request);

    assertThat(result.getBaseCurrency()).isEqualTo("EUR");
    assertThat(result.getTargetCurrency()).isEqualTo("INR");
    assertThat(result.getRate()).isGreaterThan(BigDecimal.ZERO);


  }

  @Test
  @Order(2)
  void getExchangeRate_shouldReturnFromDb_whenCacheIsMissing() {
    // Clear the cache but keep the DB entry
    String cacheKey = "EUR-INR-2025-04-18T00:00";
    exchangeRateRedisTemplate.delete(cacheKey);

    ExchangeRateRequestDTO request = new ExchangeRateRequestDTO("EUR", "INR", LocalDateTime.of(2025, 4, 18, 0, 0));

    ExchangeRateDTO result = exchangeRateService.getExchangeRate(request);

    assertThat(result.getBaseCurrency()).isEqualTo("EUR");
    assertThat(result.getTargetCurrency()).isEqualTo("INR");
  }

}
