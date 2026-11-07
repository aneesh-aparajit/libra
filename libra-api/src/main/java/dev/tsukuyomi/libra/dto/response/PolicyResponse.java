package dev.tsukuyomi.libra.dto.response;

import dev.tsukuyomi.libra.domain.enums.PolicyStatus;

public record PolicyResponse(
        Long id,
        Long tenantID,
        Long scopeID,
        String entityID,
        String code,
        String appliedJs,
        String evaluatedJs,
        PolicyStatus status,
        Long validFrom,
        Long validTo,
        Long version,
        Long createdAt,
        Long updatedAt
) {
}
