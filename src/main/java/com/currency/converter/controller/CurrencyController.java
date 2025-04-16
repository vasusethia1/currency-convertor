package com.currency.converter.controller;

import com.currency.converter.dto.ApiResponseDTO;
import com.currency.converter.dto.ConversionRequestDTO;
import com.currency.converter.dto.ExchangeRateDTO;
import com.currency.converter.dto.ExchangeRateRequestDTO;
import com.currency.converter.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
@Tag(name = "Currency Converter", description = "API for currency conversion and exchange rates")
public class CurrencyController {

        private final ExchangeRateService exchangeRateService;

        @GetMapping("/rate")
        @Operation(summary = "Get exchange rate between two currencies", description = "Retrieves the exchange rate between two currencies for a specific date. "
                        + "If no date is provided, it returns the latest available rate.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rate", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input parameters", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Exchange rate not found", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
        })
        public ResponseEntity<ApiResponseDTO<ExchangeRateDTO>> getExchangeRate(
                        @Parameter(description = "Source currency code (e.g., USD)", example = "USD") @RequestParam String sourceCurrency,
                        @Parameter(description = "Target currency code (e.g., EUR)", example = "EUR") @RequestParam String targetCurrency,
                        @Parameter(description = "Date for historical rate (optional)", example = "2025-04-16") @RequestParam(required = false) String date) {

                ExchangeRateRequestDTO request = ExchangeRateRequestDTO.builder()
                                .sourceCurrency(sourceCurrency)
                                .targetCurrency(targetCurrency)
                                .date(date != null ? java.time.LocalDate.parse(date) : java.time.LocalDate.now())
                                .build();

                ExchangeRateDTO rate = exchangeRateService.getExchangeRate(request);
                return ResponseEntity.ok(ApiResponseDTO.success(rate));
        }

        @PostMapping("/convert")
        @Operation(summary = "Convert amount from one currency to another", description = "Converts an amount from source currency to target currency using the exchange rate "
                        + "for the specified date. If no date is provided, it uses the latest available rate.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully converted amount", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input parameters", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Exchange rate not found", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
        })
        @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                        @ExampleObject(name = "Basic Conversion", value = "{\"sourceCurrency\": \"USD\", \"targetCurrency\": \"EUR\", \"amount\": 100.00}"),
                        @ExampleObject(name = "Historical Conversion", value = "{\"sourceCurrency\": \"USD\", \"targetCurrency\": \"EUR\", \"amount\": 100.00, \"date\": \"2025-04-16\"}")
        }))
        public ResponseEntity<ApiResponseDTO<BigDecimal>> convertCurrency(
                        @Valid @RequestBody ConversionRequestDTO request) {
                BigDecimal convertedAmount = exchangeRateService.convertCurrency(request);
                return ResponseEntity.ok(ApiResponseDTO.success(convertedAmount));
        }
}