package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.dto.response.PolicyResponse;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

@Component
public class PolicyResponseTransformer implements Transformer<Policy, PolicyResponse> {

    @Override
    public PolicyResponse transform(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getTenantID(),
                policy.getScopeID(),
                policy.getEntityID(),
                policy.getCode(),
                policy.getAppliedJs(),
                policy.getEvaluatedJs(),
                policy.getStatus(),
                policy.getValidFrom(),
                policy.getValidTo(),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

}
