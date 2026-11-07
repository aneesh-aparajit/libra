package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.dto.request.CreatePolicyRequest;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CreatePolicyRequestTransformer implements Transformer<CreatePolicyRequest, Policy> {

    @Override
    public Policy transform(CreatePolicyRequest request) {
        return Policy.builder()
                .tenantID(request.tenantID())
                .scopeID(request.scopeID())
                .entityID(request.entityID().trim())
                .code(request.code().trim())
                .appliedJs(request.appliedJs())
                .evaluatedJs(request.evaluatedJs())
                .status(Objects.requireNonNullElse(request.status(), PolicyStatus.ACTIVE))
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .build();
    }

}
