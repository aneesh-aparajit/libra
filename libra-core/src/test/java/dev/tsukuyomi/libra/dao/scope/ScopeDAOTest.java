package dev.tsukuyomi.libra.dao.scope;

import dev.tsukuyomi.libra.dao.PostgresDAOTestBase;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeDAOTest extends PostgresDAOTestBase {

    @Test
    void createScopeReturnsPersistedRow() {
        Tenant tenant = newTenant();

        Scope created = newScope(tenant.getId(), "payment.credit");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTenantID()).isEqualTo(tenant.getId());
        assertThat(created.getScopeName()).isEqualTo("payment.credit");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void createScopeForUnknownTenantThrows() {
        assertThatThrownBy(() -> newScope(999L, "payment.credit"))
                .isInstanceOf(LibraDAOException.class)
                .hasMessage("error creating scope");
    }

    @Test
    void scopeNameIsUniquePerTenant() {
        Tenant tenant = newTenant();
        Tenant other = newTenant();
        newScope(tenant.getId(), "payment.credit");

        assertThatThrownBy(() -> newScope(tenant.getId(), "payment.credit"))
                .isInstanceOf(LibraDAOException.class);
        // same name under another tenant is fine
        assertThat(newScope(other.getId(), "payment.credit").getId()).isNotNull();
    }

    @Test
    void getScopesByTenantID() {
        Tenant tenant = newTenant();
        Tenant other = newTenant();
        Scope a = newScope(tenant.getId(), "a");
        Scope b = newScope(tenant.getId(), "b");
        newScope(other.getId(), "c");

        assertThat(scopeDAO.getScopesByTenantID(tenant.getId())).containsExactlyInAnyOrder(a, b);
        assertThat(scopeDAO.getScopesByTenantID(999L)).isEmpty();
    }

    @Test
    void getScopeByID() {
        Scope scope = newScope(newTenant().getId(), "a");

        assertThat(scopeDAO.getScopeByID(scope.getId())).contains(scope);
        assertThat(scopeDAO.getScopeByID(scope.getId() + 1)).isEmpty();
    }

    @Test
    void getScopeByIDAndScopeName() {
        Scope scope = newScope(newTenant().getId(), "a");

        assertThat(scopeDAO.getScopeByIDAndScopeName(scope.getId(), "a")).contains(scope);
        assertThat(scopeDAO.getScopeByIDAndScopeName(scope.getId(), "b")).isEmpty();
    }

}
