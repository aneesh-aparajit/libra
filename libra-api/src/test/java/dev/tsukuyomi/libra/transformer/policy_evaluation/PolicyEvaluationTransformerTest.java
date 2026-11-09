package dev.tsukuyomi.libra.transformer.policy_evaluation;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.dto.request.EvaluatePolicyRequest;
import dev.tsukuyomi.libra.dto.response.PolicyEvaluationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEvaluationTransformerTest {

    private final EvaluatePolicyRequestTransformer requestTransformer = new EvaluatePolicyRequestTransformer();
    private final PolicyEvaluationResponseTransformer responseTransformer = new PolicyEvaluationResponseTransformer();

    @Test
    void requestTrimsEntityAndKeepsInput() {
        PolicyEvaluationDTO dto = requestTransformer.transform(
                new EvaluatePolicyRequest(1L, 2L, " user-1 ", Map.of("amount", 5)));

        assertThat(dto.getTenantID()).isEqualTo(1L);
        assertThat(dto.getScopeID()).isEqualTo(2L);
        assertThat(dto.getEntityID()).isEqualTo("user-1");
        assertThat(dto.getInputAttrs()).isEqualTo(Map.of("amount", 5));
        assertThat(dto.getRequestID()).isNull();
    }

    @Test
    void requestDefaultsMissingInputToEmpty() {
        PolicyEvaluationDTO dto = requestTransformer.transform(new EvaluatePolicyRequest(1L, 2L, "user-1", null));

        assertThat(dto.getInputAttrs()).isEmpty();
    }

    @Test
    void responseMapsAllFields() {
        PolicyEvaluationDTO dto = PolicyEvaluationDTO.builder()
                .tenantID(1L).scopeID(2L).entityID("user-1")
                .requestID("req-1")
                .evaluationResult(EvaluationResult.DENY)
                .deniedPolicies(List.of("broken"))
                .evaluationResults(List.of(PolicyEvaluationResultDTO.builder()
                        .policyID(12L).policyCode("broken").entityID("*")
                        .result(EvaluationResult.DENY).errors(Map.of("msg", "boom"))
                        .build()))
                .build();

        assertThat(responseTransformer.transform(dto)).isEqualTo(new PolicyEvaluationResponse(
                "req-1", 1L, 2L, "user-1", EvaluationResult.DENY, List.of("broken"),
                List.of(new PolicyEvaluationResponse.PolicyResult(
                        12L, "broken", "*", EvaluationResult.DENY, Map.of("msg", "boom")))));
    }

    @Test
    void responseDefaultsNullListsToEmpty() {
        PolicyEvaluationDTO dto = PolicyEvaluationDTO.builder()
                .requestID("req-1").evaluationResult(EvaluationResult.ALLOW)
                .build();

        PolicyEvaluationResponse response = responseTransformer.transform(dto);

        assertThat(response.deniedPolicies()).isEmpty();
        assertThat(response.evaluations()).isEmpty();
    }

}
