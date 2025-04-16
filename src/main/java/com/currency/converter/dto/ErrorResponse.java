package com.currency.converter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response structure")
public class ErrorResponse {
    @Schema(description = "Error code that identifies the type of error", example = "EXCHANGE_RATE_NOT_FOUND", required = true)
    private String code;

    @Schema(description = "Human-readable error message explaining what went wrong", example = "Exchange rate not found for USD to EUR on date 2024-04-15", required = true)
    private String message;

    @Schema(description = "Additional details about the error (optional)", example = "No historical rates available before the specified date", required = false)
    private String details;
}