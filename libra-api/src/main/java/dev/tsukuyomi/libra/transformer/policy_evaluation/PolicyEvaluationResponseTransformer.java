package dev.tsukuyomi.libra.transformer.policy_evaluation;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationResultDTO;
import dev.tsukuyomi.libra.dto.response.PolicyEvaluationResponse;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class PolicyEvaluationResponseTransformer implements Transformer<PolicyEvaluationDTO, PolicyEvaluationResponse> {

    @Override
    public PolicyEvaluationResponse transform(PolicyEvaluationDTO dto) {
        List<PolicyEvaluationResponse.PolicyResult> evaluations = Objects.requireNonNullElse(
                        dto.getEvaluationResults(), List.<PolicyEvaluationResultDTO>of())
                .stream()
                .map(PolicyEvaluationResponseTransformer::toPolicyResult)
                .toList();
        return new PolicyEvaluationResponse(
                dto.getRequestID(),
                dto.getTenantID(),
                dto.getScopeID(),
                dto.getEntityID(),
                dto.getEvaluationResult(),
                Objects.requireNonNullElse(dto.getDeniedPolicies(), List.of()),
                evaluations
        );
    }

    private static PolicyEvaluationResponse.PolicyResult toPolicyResult(PolicyEvaluationResultDTO result) {
        return new PolicyEvaluationResponse.PolicyResult(
                result.getPolicyID(),
                result.getPolicyCode(),
                result.getEntityID(),
                result.getResult(),
                result.getErrors()
        );
    }

}
