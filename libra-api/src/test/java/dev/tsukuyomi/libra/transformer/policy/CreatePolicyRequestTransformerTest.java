package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.dto.request.CreatePolicyRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePolicyRequestTransformerTest {

    private final CreatePolicyRequestTransformer transformer = new CreatePolicyRequestTransformer();

    private static CreatePolicyRequest request(PolicyStatus status, String appliedJs, Long validTo) {
        return new CreatePolicyRequest(1L, 2L, " user-1 ", " limit ", appliedJs, "evaluate()", status, 1000L, validTo);
    }

    @Test
    void mapsAllFields() {
        Policy policy = transformer.transform(request(PolicyStatus.INACTIVE, "applies()", 2000L));

        assertThat(policy.getTenantID()).isEqualTo(1L);
        assertThat(policy.getScopeID()).isEqualTo(2L);
        assertThat(policy.getAppliedJs()).isEqualTo("applies()");
        assertThat(policy.getEvaluatedJs()).isEqualTo("evaluate()");
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
        assertThat(policy.getValidFrom()).isEqualTo(1000L);
        assertThat(policy.getValidTo()).isEqualTo(2000L);
    }

    @Test
    void trimsEntityIDAndCode() {
        Policy policy = transformer.transform(request(PolicyStatus.ACTIVE, null, null));

        assertThat(policy.getEntityID()).isEqualTo("user-1");
        assertThat(policy.getCode()).isEqualTo("limit");
    }

    @Test
    void doesNotTrimScripts() {
        CreatePolicyRequest request = new CreatePolicyRequest(
                1L, 2L, "*", "limit", "  applies()\n", "\n  evaluate()  ", PolicyStatus.ACTIVE, 1000L, null);

        Policy policy = transformer.transform(request);

        assertThat(policy.getAppliedJs()).isEqualTo("  applies()\n");
        assertThat(policy.getEvaluatedJs()).isEqualTo("\n  evaluate()  ");
    }

    @Test
    void defaultsStatusToActive() {
        assertThat(transformer.transform(request(null, null, null)).getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    @Test
    void keepsOptionalFieldsNull() {
        Policy policy = transformer.transform(request(PolicyStatus.ACTIVE, null, null));

        assertThat(policy.getAppliedJs()).isNull();
        assertThat(policy.getValidTo()).isNull();
    }

    @Test
    void leavesServerManagedFieldsUnset() {
        Policy policy = transformer.transform(request(PolicyStatus.ACTIVE, null, null));

        assertThat(policy.getId()).isNull();
        assertThat(policy.getVersion()).isNull();
        assertThat(policy.getCreatedAt()).isNull();
        assertThat(policy.getUpdatedAt()).isNull();
    }

}
