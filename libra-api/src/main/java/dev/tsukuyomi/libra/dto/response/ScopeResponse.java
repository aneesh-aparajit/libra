package dev.tsukuyomi.libra.dto.response;

public record ScopeResponse(
        Long id,
        Long tenantID,
        String scopeName,
        Long createdAt,
        Long updatedAt
) {
}
