package dev.tsukuyomi.libra.dao.scope;

import dev.tsukuyomi.libra.dao.FakeResultSet;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeRowMapperTest {

    private final ScopeRowMapper mapper = new ScopeRowMapper();

    @Test
    void mapsAllColumns() throws Exception {
        Scope scope = mapper.mapRow(FakeResultSet.of(Map.of(
                "id", 3L,
                "tenant_id", 7L,
                "scope_name", "payment.credit",
                "created_at", 1_000L,
                "updated_at", 2_000L
        )), 0);

        assertThat(scope).isEqualTo(Scope.builder()
                .id(3L)
                .tenantID(7L)
                .ScopeName("payment.credit")
                .createdAt(1_000L)
                .updatedAt(2_000L)
                .build());
    }

    @Test
    void wrapsSqlException() throws Exception {
        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.failing(), 0))
                .isInstanceOf(LibraDAOException.class);
    }

}
