package dev.tsukuyomi.libra.dao.tenant;

import dev.tsukuyomi.libra.dao.FakeResultSet;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantRowMapperTest {

    private final TenantRowMapper mapper = new TenantRowMapper();

    @Test
    void mapsAllColumns() throws Exception {
        Tenant tenant = mapper.mapRow(FakeResultSet.of(Map.of(
                "id", 7L,
                "tenant_code", "acme",
                "tenant_name", "Acme Corp",
                "created_at", 1_000L,
                "updated_at", 2_000L
        )), 0);

        assertThat(tenant).isEqualTo(Tenant.builder()
                .id(7L)
                .tenantCode("acme")
                .tenantName("Acme Corp")
                .createdAt(1_000L)
                .updatedAt(2_000L)
                .build());
    }

    @Test
    void missingColumnsMapToNull() throws Exception {
        Tenant tenant = mapper.mapRow(FakeResultSet.of(Map.of("id", 1L)), 0);

        assertThat(tenant.getId()).isEqualTo(1L);
        assertThat(tenant.getTenantCode()).isNull();
        assertThat(tenant.getCreatedAt()).isNull();
    }

    @Test
    void wrapsSqlException() throws Exception {
        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.failing(), 0))
                .isInstanceOf(LibraDAOException.class)
                .hasMessageContaining("id");
    }

}
