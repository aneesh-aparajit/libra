package dev.tsukuyomi.libra.dao.policy_evaluation;

import dev.tsukuyomi.libra.dao.FakeResultSet;
import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyEvaluationRowMapperTest {

    private final PolicyEvaluationRowMapper mapper = new PolicyEvaluationRowMapper();

    private static Map<String, Object> fullRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 21L);
        row.put("request_id", "req-1");
        row.put("entity_id", "user-1");
        row.put("policy_id", 11L);
        row.put("input_attrs", "{\"amount\": 250, \"currency\": \"INR\", \"tags\": [\"a\", \"b\"]}");
        row.put("result", "DENY");
        row.put("errors", "{\"message\": \"limit exceeded\"}");
        row.put("created_at", 1_000L);
        row.put("updated_at", 2_000L);
        return row;
    }

    @Test
    void mapsAllColumns() throws Exception {
        PolicyEvaluation evaluation = mapper.mapRow(FakeResultSet.of(fullRow()), 0);

        assertThat(evaluation.getId()).isEqualTo(21L);
        assertThat(evaluation.getRequestID()).isEqualTo("req-1");
        assertThat(evaluation.getEntityID()).isEqualTo("user-1");
        assertThat(evaluation.getPolicyID()).isEqualTo(11L);
        assertThat(evaluation.getResult()).isEqualTo(EvaluationResult.DENY);
        assertThat(evaluation.getInputAttrs())
                .containsEntry("amount", 250L) // integers stay Long, not Double
                .containsEntry("currency", "INR")
                .containsEntry("tags", List.of("a", "b"));
        assertThat(evaluation.getErrors()).containsEntry("message", "limit exceeded");
        assertThat(evaluation.getCreatedAt()).isEqualTo(1_000L);
        assertThat(evaluation.getUpdatedAt()).isEqualTo(2_000L);
    }

    @Test
    void nullJsonColumnsMapToNull() throws Exception {
        Map<String, Object> row = fullRow();
        row.put("input_attrs", null);
        row.put("errors", null);

        PolicyEvaluation evaluation = mapper.mapRow(FakeResultSet.of(row), 0);

        assertThat(evaluation.getInputAttrs()).isNull();
        assertThat(evaluation.getErrors()).isNull();
    }

    @Test
    void invalidJsonThrows() throws Exception {
        Map<String, Object> row = fullRow();
        row.put("input_attrs", "{not json");

        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.of(row), 0))
                .isInstanceOf(LibraDAOException.class)
                .hasMessageContaining("input_attrs");
    }

    @Test
    void unknownResultThrows() throws Exception {
        Map<String, Object> row = fullRow();
        row.put("result", "MAYBE");

        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.of(row), 0))
                .isInstanceOf(LibraDAOException.class)
                .hasMessageContaining("MAYBE");
    }

    @Test
    void wrapsSqlException() throws Exception {
        assertThatThrownBy(() -> mapper.mapRow(FakeResultSet.failing(), 0))
                .isInstanceOf(LibraDAOException.class);
    }

}
