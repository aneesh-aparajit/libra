package dev.tsukuyomi.libra.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateScopeRequest(
        @NotNull @Positive Long tenantID,
        @NotBlank @Size(max = 255) String scopeName
) {
}
