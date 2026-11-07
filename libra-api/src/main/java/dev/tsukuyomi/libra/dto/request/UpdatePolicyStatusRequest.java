package dev.tsukuyomi.libra.dto.request;

import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePolicyStatusRequest(@NotNull PolicyStatus status) {
}
