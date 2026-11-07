package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.dto.request.UpdatePolicyRequest;
import dev.tsukuyomi.libra.transformer.Transformer;
import org.springframework.stereotype.Component;

/**
 * Maps the mutable fields of a policy. The caller sets the id from the path.
 */
@Component
public class UpdatePolicyRequestTransformer implements Transformer<UpdatePolicyRequest, Policy> {

    @Override
    public Policy transform(UpdatePolicyRequest request) {
        return Policy.builder()
                .appliedJs(request.appliedJs())
                .evaluatedJs(request.evaluatedJs())
                .status(request.status())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .version(request.version())
                .build();
    }

}
