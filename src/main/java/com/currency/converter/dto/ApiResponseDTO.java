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
@Schema(description = "Standard API response structure")
public class ApiResponseDTO<T> {
    @Schema(description = "Status of the response", example = "success")
    private String status;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Error information if any")
    private ErrorResponse error;

    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    public static <T> ApiResponseDTO<T> error(String errorCode, String message) {
        return ApiResponseDTO.<T>builder()
                .status("error")
                .error(ErrorResponse.builder()
                        .code(errorCode)
                        .message(message)
                        .build())
                .build();
    }
}