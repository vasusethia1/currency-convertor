package com.zeref.currencyconvertor.integration;


import com.currency.converter.CurrencyConverterApplication;
import com.currency.converter.dto.ExchangeRateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CurrencyConverterApplication.class)
@ExtendWith(SpringExtension.class)
@Testcontainers
public class RedisIntegrationTest {

  @Autowired
  ObjectMapper objectMapper;

  @Container
  static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.redis.host", redisContainer::getHost);
    registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));
  }

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Test
  void testRedisSetAndGet() {
    redisTemplate.opsForValue().set("test-key", "test-value");
    String value = redisTemplate.opsForValue().get("test-key");

    assertThat(value).isEqualTo("test-value");
  }

  @Autowired
  @Qualifier("exchangeRateRedisTemplate")
  private RedisTemplate<String, ExchangeRateDTO> exchangeRateRedisTemplate;

  @Test
  void testStoreExchangeRateDTO() {
    ExchangeRateDTO dto = ExchangeRateDTO.builder()
      .baseCurrency("USD")
      .targetCurrency("INR")
      .rate(BigDecimal.valueOf(82.12))
      .date(LocalDateTime.now())
      .timestamp(System.currentTimeMillis())
      .build();

    exchangeRateRedisTemplate.opsForValue().set("USD-INR", dto);
    Object cached = exchangeRateRedisTemplate.opsForValue().get("USD-INR");
    ExchangeRateDTO exchangeRateDTOC = objectMapper.convertValue(cached, ExchangeRateDTO.class);
    assertThat(cached).isNotNull();
    assertThat(exchangeRateDTOC.getRate()).isEqualTo(BigDecimal.valueOf(82.12));
  }

}
