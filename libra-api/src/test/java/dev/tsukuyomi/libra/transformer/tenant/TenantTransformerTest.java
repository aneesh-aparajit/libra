package dev.tsukuyomi.libra.transformer.tenant;

import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.dto.request.CreateTenantRequest;
import dev.tsukuyomi.libra.dto.response.TenantResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTransformerTest {

    private final TenantRequestTransformer requestTransformer = new TenantRequestTransformer();
    private final TenantResponseTransformer responseTransformer = new TenantResponseTransformer();

    @Test
    void requestMapsAndTrimsFields() {
        Tenant tenant = requestTransformer.transform(new CreateTenantRequest("  acme ", "\tAcme Corp\n"));

        assertThat(tenant.getTenantCode()).isEqualTo("acme");
        assertThat(tenant.getTenantName()).isEqualTo("Acme Corp");
    }

    @Test
    void requestLeavesServerManagedFieldsUnset() {
        Tenant tenant = requestTransformer.transform(new CreateTenantRequest("acme", "Acme"));

        assertThat(tenant.getId()).isNull();
        assertThat(tenant.getCreatedAt()).isNull();
        assertThat(tenant.getUpdatedAt()).isNull();
    }

    @Test
    void responseMapsAllFields() {
        Tenant tenant = Tenant.builder()
                .id(7L).tenantCode("acme").tenantName("Acme Corp").createdAt(100L).updatedAt(200L)
                .build();

        assertThat(responseTransformer.transform(tenant))
                .isEqualTo(new TenantResponse(7L, "acme", "Acme Corp", 100L, 200L));
    }

    @Test
    void responsePassesNullsThrough() {
        Tenant tenant = Tenant.builder().id(7L).build();

        assertThat(responseTransformer.transform(tenant))
                .isEqualTo(new TenantResponse(7L, null, null, null, null));
    }

}
