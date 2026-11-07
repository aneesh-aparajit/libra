package dev.tsukuyomi.libra.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 255) String tenantCode,
        @NotBlank @Size(max = 255) String tenantName
) {
}
