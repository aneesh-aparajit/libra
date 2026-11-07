package dev.tsukuyomi.libra.dto.request;

import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePolicyRequest(
        String appliedJs,
        @NotBlank String evaluatedJs,
        @NotNull PolicyStatus status,
        @NotNull Long validFrom,
        Long validTo,
        @NotNull @Positive Long version
) {

    @AssertTrue(message = "validTo must be after validFrom")
    public boolean isValidWindow() {
        return validFrom == null || validTo == null || validTo > validFrom;
    }

}
