package dev.tsukuyomi.libra.transformer.policy_evaluation;

import dev.tsukuyomi.libra.domain.dto.PolicyEvaluationDTO;
import dev.tsukuyomi.libra.dto.request.EvaluatePolicyRequest;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class EvaluatePolicyRequestTransformer implements Transformer<EvaluatePolicyRequest, PolicyEvaluationDTO> {

    @Override
    public PolicyEvaluationDTO transform(EvaluatePolicyRequest request) {
        return PolicyEvaluationDTO.builder()
                .tenantID(request.tenantID())
                .scopeID(request.scopeID())
                .entityID(request.entityID().trim())
                .inputAttrs(Objects.requireNonNullElseGet(request.inputAttrs(), Map::of))
                .build();
    }

}
