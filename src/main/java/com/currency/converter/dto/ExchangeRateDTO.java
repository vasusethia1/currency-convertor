package com.currency.converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rate information")
public class ExchangeRateDTO {

    @Schema(description = "Base currency code", example = "EUR", pattern = "^[A-Z]{3}$")
    private String baseCurrency;

    @Schema(description = "Target currency code", example = "USD", pattern = "^[A-Z]{3}$")
    private String targetCurrency;

    @Schema(description = "Exchange rate from base to target currency", example = "1.23456", minimum = "0.000001")
    private BigDecimal rate;

    @Schema(description = "Date of the exchange rate", example = "2024-04-15")
    private LocalDate date;

    @Schema(description = "Timestamp of when the rate was fetched", example = "1713206400")
    private Long timestamp;
}