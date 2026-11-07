package dev.tsukuyomi.libra.dao.policy;

import dev.tsukuyomi.libra.dao.FakeResultSet;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyRowMapperTest {

    private final PolicyRowMapper mapper = new PolicyRowMapper();

    private static Map<String, Object> fullRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 11L);
        row.put("tenant_id", 7L);
        row.put("scope_id", 3L);
        row.put("entity_id", "user-1");
        row.put("code", "no-sms-after-22");
        row.put("applied_js", "function applies(){return true;}");
        row.put("evaluate_js", "function evaluate(){return true;}");
        row.put("status", "ACTIVE");
        row.put("valid_from", 100L);
        row.put("valid_till", 200L);
        row.put("version", 2);
        row.put("created_at", 1_000L);
        row.put("updated_at", 2_000L);
        return row;
    }

    @Test
    void mapsAllColumns() throws Exception {
        Policy policy = mapper.mapRow(FakeResultSet.of(fullRow()), 0);

        assertThat(policy).isEqualTo(Policy.builder()
                .id(11L)
                .tenantID(7L)
                .scopeID(3L)
                .entityID("user-1")
                .code("no-sms-after-22")
                .appliedJs("function applies(){return true;}")
                .evaluatedJs("function evaluate(){return true;}")
                .status(PolicyStatus.ACTIVE)
                .validFrom(100L)
                .validTo(200L)
                .version(2L)
                .createdAt(1_000L)
                .updatedAt(2_000L)
                .build());
    }

    @Test
    void nullableColumnsMapToNull() throws Exception {
        Map<String, Object> row = fullRow();
        row.put("applied_js", null);
        row.put("valid_till", null);

        Policy policy = mapper.mapRow(FakeResultSet.of(row), 0);

        assertThat(policy.getAppliedJs()).isNull();
        assertThat(policy.getValidTo()).isNull();
        // a NULL read must not leak into the next column's wasNull()
        assertThat(policy.getVersion()).isEqualTo(2L);
    }

    @Test
    void mapsEveryStatus() throws Exception {
        for (PolicyStatus status : PolicyStatus.values()) {
            Map<String, Object> row = fullRow();
            row.put("status", status.name());
            assertThat(mapper.mapRow(FakeResultSet.of(row), 0).getStatus()).isEqualTo(status);
        }
    }

    @Test
    void unknownStatusThrows() throws Exception {
        Map<String, Object> row = fullRow();
        row.put("status", "DELETED");

        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.of(row), 0))
                .isInstanceOf(LibraDAOException.class)
                .hasMessageContaining("DELETED");
    }

    @Test
    void wrapsSqlException() throws Exception {
        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.failing(), 0))
                .isInstanceOf(LibraDAOException.class);
    }

}
