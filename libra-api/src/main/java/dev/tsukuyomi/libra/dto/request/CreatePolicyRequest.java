package dev.tsukuyomi.libra.dto.request;

import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePolicyRequest(
        @NotNull @Positive Long tenantID,
        @NotNull @Positive Long scopeID,
        @NotBlank @Size(max = 255) String entityID,
        @NotBlank @Size(max = 255) String code,
        String appliedJs,
        @NotBlank String evaluatedJs,
        PolicyStatus status,
        @NotNull Long validFrom,
        Long validTo
) {

    @AssertTrue(message = "validTo must be after validFrom")
    public boolean isValidWindow() {
        return validFrom == null || validTo == null || validTo > validFrom;
    }

    @AssertTrue(message = "status cannot be EXPIRED on create")
    public boolean isCreatableStatus() {
        return status != PolicyStatus.EXPIRED;
    }

}
