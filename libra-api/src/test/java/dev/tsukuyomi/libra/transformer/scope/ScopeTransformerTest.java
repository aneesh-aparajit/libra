package dev.tsukuyomi.libra.transformer.scope;

import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.dto.request.CreateScopeRequest;
import dev.tsukuyomi.libra.dto.response.ScopeResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeTransformerTest {

    private final ScopeRequestTransformer requestTransformer = new ScopeRequestTransformer();
    private final ScopeResponseTransformer responseTransformer = new ScopeResponseTransformer();

    @Test
    void requestMapsAndTrimsFields() {
        Scope scope = requestTransformer.transform(new CreateScopeRequest(1L, "  payment.credit "));

        assertThat(scope.getTenantID()).isEqualTo(1L);
        assertThat(scope.getScopeName()).isEqualTo("payment.credit");
    }

    @Test
    void requestLeavesServerManagedFieldsUnset() {
        Scope scope = requestTransformer.transform(new CreateScopeRequest(1L, "payment.credit"));

        assertThat(scope.getId()).isNull();
        assertThat(scope.getCreatedAt()).isNull();
        assertThat(scope.getUpdatedAt()).isNull();
    }

    @Test
    void responseMapsAllFields() {
        Scope scope = Scope.builder()
                .id(3L).tenantID(1L).ScopeName("payment.credit").createdAt(100L).updatedAt(200L)
                .build();

        assertThat(responseTransformer.transform(scope))
                .isEqualTo(new ScopeResponse(3L, 1L, "payment.credit", 100L, 200L));
    }

    @Test
    void responsePassesNullsThrough() {
        Scope scope = Scope.builder().id(3L).build();

        assertThat(responseTransformer.transform(scope))
                .isEqualTo(new ScopeResponse(3L, null, null, null, null));
    }

}
