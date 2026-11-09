package dev.tsukuyomi.libra.dto.response;

import dev.tsukuyomi.libra.domain.enums.EvaluationResult;

import java.util.List;
import java.util.Map;

/**
 * Overall decision for an evaluate call. {@code result} is {@code ALLOW} only when every applicable
 * policy allowed; {@code deniedPolicies} lists the codes of those that denied or errored.
 */
public record PolicyEvaluationResponse(
        String requestID,
        Long tenantID,
        Long scopeID,
        String entityID,
        EvaluationResult result,
        List<String> deniedPolicies,
        List<PolicyResult> evaluations
) {

    /**
     * Outcome of a single applicable policy.
     */
    public record PolicyResult(
            Long policyID,
            String policyCode,
            String entityID,
            EvaluationResult result,
            Map<String, Object> errors
    ) {
    }

}
