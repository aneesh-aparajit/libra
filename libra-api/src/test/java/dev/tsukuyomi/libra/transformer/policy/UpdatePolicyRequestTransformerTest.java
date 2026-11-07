package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.dto.request.UpdatePolicyRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatePolicyRequestTransformerTest {

    private final UpdatePolicyRequestTransformer transformer = new UpdatePolicyRequestTransformer();

    @Test
    void mapsMutableFieldsAndVersion() {
        Policy policy = transformer.transform(
                new UpdatePolicyRequest("applies()", "evaluate()", PolicyStatus.INACTIVE, 1000L, 3000L, 4L));

        assertThat(policy.getAppliedJs()).isEqualTo("applies()");
        assertThat(policy.getEvaluatedJs()).isEqualTo("evaluate()");
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
        assertThat(policy.getValidFrom()).isEqualTo(1000L);
        assertThat(policy.getValidTo()).isEqualTo(3000L);
        assertThat(policy.getVersion()).isEqualTo(4L);
    }

    @Test
    void keepsOptionalFieldsNull() {
        Policy policy = transformer.transform(
                new UpdatePolicyRequest(null, "evaluate()", PolicyStatus.ACTIVE, 1000L, null, 1L));

        assertThat(policy.getAppliedJs()).isNull();
        assertThat(policy.getValidTo()).isNull();
    }

    @Test
    void leavesIdentityAndImmutableFieldsUnset() {
        // the controller sets the id from the path; tenant, scope, entity and code cannot change on update
        Policy policy = transformer.transform(
                new UpdatePolicyRequest(null, "evaluate()", PolicyStatus.ACTIVE, 1000L, null, 1L));

        assertThat(policy.getId()).isNull();
        assertThat(policy.getTenantID()).isNull();
        assertThat(policy.getScopeID()).isNull();
        assertThat(policy.getEntityID()).isNull();
        assertThat(policy.getCode()).isNull();
        assertThat(policy.getCreatedAt()).isNull();
        assertThat(policy.getUpdatedAt()).isNull();
    }

}
