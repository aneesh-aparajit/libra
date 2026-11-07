package dev.tsukuyomi.libra.dto.response;

public record TenantResponse(
        Long id,
        String tenantCode,
        String tenantName,
        Long createdAt,
        Long updatedAt
) {
}
