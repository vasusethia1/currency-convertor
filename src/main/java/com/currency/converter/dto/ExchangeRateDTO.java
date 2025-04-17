package com.currency.converter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Exchange rate information")
public class ExchangeRateDTO implements Serializable {


    private static final long serialVersionUID = 1L;

    @Schema(description = "Base currency code", example = "EUR", pattern = "^[A-Z]{3}$")
    private String baseCurrency;

    @Schema(description = "Target currency code", example = "USD", pattern = "^[A-Z]{3}$")
    private String targetCurrency;

    @Schema(description = "Exchange rate from base to target currency", example = "1.23456", minimum = "0.000001")
    private BigDecimal rate;

    @Schema(description = "Date of the exchange rate", example = "2024-04-15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    @Schema(description = "Timestamp of when the rate was fetched", example = "1713206400")
    private Long timestamp;
  }
