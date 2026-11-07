package dev.tsukuyomi.libra.transformer.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.dto.response.PolicyResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class curlPolicyResponseTransformerTest {

    private final PolicyResponseTransformer transformer = new PolicyResponseTransformer();

    @Test
    void mapsAllFields() {
        Policy policy = Policy.builder()
                .id(5L).tenantID(1L).scopeID(2L).entityID("user-1").code("limit")
                .appliedJs("applies()").evaluatedJs("evaluate()").status(PolicyStatus.EXPIRED)
                .validFrom(1000L).validTo(2000L).version(3L).createdAt(10L).updatedAt(20L)
                .build();

        assertThat(transformer.transform(policy)).isEqualTo(new PolicyResponse(
                5L, 1L, 2L, "user-1", "limit", "applies()", "evaluate()", PolicyStatus.EXPIRED,
                1000L, 2000L, 3L, 10L, 20L));
    }

    @Test
    void passesNullsThrough() {
        Policy policy = Policy.builder().id(5L).build();

        assertThat(transformer.transform(policy)).isEqualTo(new PolicyResponse(
                5L, null, null, null, null, null, null, null, null, null, null, null, null));
    }

}
