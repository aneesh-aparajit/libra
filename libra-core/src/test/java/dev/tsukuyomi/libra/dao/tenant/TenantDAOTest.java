package dev.tsukuyomi.libra.dao.tenant;

import dev.tsukuyomi.libra.dao.PostgresDAOTestBase;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantDAOTest extends PostgresDAOTestBase {

    @Test
    void createTenantReturnsPersistedRow() {
        long before = System.currentTimeMillis();

        Tenant created = tenantDAO.createTenant(Tenant.builder().tenantCode("acme").tenantName("Acme").build());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTenantCode()).isEqualTo("acme");
        assertThat(created.getTenantName()).isEqualTo("Acme");
        // timestamps are filled by the db trigger
        assertThat(created.getCreatedAt()).isGreaterThanOrEqualTo(before);
        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
    }

    @Test
    void createTenantWithMissingNameThrows() {
        assertThatThrownBy(() -> tenantDAO.createTenant(Tenant.builder().tenantCode("acme").build()))
                .isInstanceOf(LibraDAOException.class)
                .hasMessage("error creating tenant");
    }

    @Test
    void getTenantByID() {
        Tenant created = newTenant();

        assertThat(tenantDAO.getTenantByID(created.getId())).contains(created);
        assertThat(tenantDAO.getTenantByID(created.getId() + 1)).isEmpty();
    }

    @Test
    void getTenantByCode() {
        Tenant created = newTenant();

        assertThat(tenantDAO.getTenantByCode(created.getTenantCode())).contains(created);
        assertThat(tenantDAO.getTenantByCode("missing")).isEmpty();
    }

}
