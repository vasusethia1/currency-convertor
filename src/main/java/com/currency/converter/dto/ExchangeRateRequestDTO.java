package com.currency.converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for getting exchange rate between any two currencies")
public class ExchangeRateRequestDTO {

    @NotBlank(message = "Source currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Source currency code must be 3 uppercase letters")
    @Schema(description = "Source currency code (e.g., INR)", example = "INR")
    private String sourceCurrency;

    @NotBlank(message = "Target currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Target currency code must be 3 uppercase letters")
    @Schema(description = "Target currency code (e.g., USD)", example = "USD")
    private String targetCurrency;

    @Schema(description = "Date for historical rate (optional)", example = "2024-04-15")
    private LocalDate date;
}