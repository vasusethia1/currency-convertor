package com.currency.converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for currency conversion")
public class ConversionRequestDTO {

    @NotBlank(message = "Source currency is required")
    @Schema(description = "Source currency code (e.g., USD, EUR, INR)", example = "USD", pattern = "^[A-Z]{3}$")
    private String sourceCurrency;

    @NotBlank(message = "Target currency is required")
    @Schema(description = "Target currency code (e.g., USD, EUR, INR)", example = "EUR", pattern = "^[A-Z]{3}$")
    private String targetCurrency;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to convert", example = "100.00", minimum = "0.01")
    private BigDecimal amount;

    @PastOrPresent(message = "Date cannot be in the future")
    @Schema(description = "Date for historical conversion (optional, defaults to current date)", example = "2024-04-15")
    private LocalDateTime date;
}
