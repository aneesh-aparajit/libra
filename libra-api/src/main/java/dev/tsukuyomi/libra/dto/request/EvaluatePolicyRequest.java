package dev.tsukuyomi.libra.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Asks libra whether {@code entityID} may perform the request described by {@code inputAttrs}
 * within the given tenant and scope.
 */
public record EvaluatePolicyRequest(
        @NotNull @Positive Long tenantID,
        @NotNull @Positive Long scopeID,
        @NotBlank @Size(max = 255) String entityID,
        Map<String, Object> inputAttrs
) {
}
