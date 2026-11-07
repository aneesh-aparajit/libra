package dev.tsukuyomi.libra.dao.policy_evaluation;

import dev.tsukuyomi.libra.dao.PostgresDAOTestBase;
import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.ALL_ENTITIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyEvaluationDAOTest extends PostgresDAOTestBase {

    private Long policyID;
    private Long otherPolicyID;

    @BeforeEach
    void setUp() {
        Long tenantID = newTenant().getId();
        Long scopeID = newScope(tenantID, "payment.credit").getId();
        policyID = newPolicy(tenantID, scopeID, "user-1").getId();
        otherPolicyID = newPolicy(tenantID, scopeID, ALL_ENTITIES).getId();
    }

    private PolicyEvaluation.PolicyEvaluationBuilder evaluation(String requestID, String entityID, Long policyID) {
        return PolicyEvaluation.builder()
                .requestID(requestID)
                .entityID(entityID)
                .policyID(policyID)
                .inputAttrs(Map.of("amount", 250L, "currency", "INR"))
                .result(EvaluationResult.ALLOW);
    }

    private Integer rowCount() {
        return jdbc.queryForObject("SELECT count(*) FROM policy_evaluations", Integer.class);
    }

    @Test
    void createPolicyEvaluationRoundTripsJson() {
        PolicyEvaluation created = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-1", "user-1", policyID)
                .result(EvaluationResult.DENY)
                .inputAttrs(Map.of("amount", 250L, "nested", Map.of("flag", true), "tags", List.of("a", "b")))
                .errors(Map.of("message", "limit exceeded"))
                .build());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getRequestID()).isEqualTo("req-1");
        assertThat(created.getEntityID()).isEqualTo("user-1");
        assertThat(created.getPolicyID()).isEqualTo(policyID);
        assertThat(created.getResult()).isEqualTo(EvaluationResult.DENY);
        assertThat(created.getInputAttrs())
                .containsEntry("amount", 250L)
                .containsEntry("nested", Map.of("flag", true))
                .containsEntry("tags", List.of("a", "b"));
        assertThat(created.getErrors()).containsEntry("message", "limit exceeded");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
        // stored as real jsonb, queryable from SQL
        assertThat(jdbc.queryForObject(
                "SELECT input_attrs ->> 'amount' FROM policy_evaluations WHERE id = ?", String.class, created.getId()))
                .isEqualTo("250");
    }

    @Test
    void createPolicyEvaluationWithNullJson() {
        PolicyEvaluation created = policyEvaluationDAO.createPolicyEvaluation(
                evaluation("req-1", "user-1", policyID).inputAttrs(null).errors(null).build());

        assertThat(created.getInputAttrs()).isNull();
        assertThat(created.getErrors()).isNull();
    }

    @Test
    void createPolicyEvaluationForUnknownPolicyThrows() {
        assertThatThrownBy(() -> policyEvaluationDAO.createPolicyEvaluation(evaluation("req-1", "user-1", 999L).build()))
                .isInstanceOf(LibraDAOException.class)
                .hasMessage("error creating policy evaluation");
    }

    @Test
    void createPolicyEvaluationWithoutResultThrows() {
        assertThatThrownBy(() -> policyEvaluationDAO.createPolicyEvaluation(
                evaluation("req-1", "user-1", policyID).result(null).build()))
                .isInstanceOf(LibraDAOException.class);
    }

    @Test
    void createPolicyEvaluationsInsertsAllInOrder() {
        List<PolicyEvaluation> created = policyEvaluationDAO.createPolicyEvaluations(List.of(
                evaluation("req-1", "user-1", policyID).build(),
                evaluation("req-1", "user-1", otherPolicyID)
                        .result(EvaluationResult.DENY)
                        .errors(Map.of("message", "boom"))
                        .build()
        ));

        assertThat(created).hasSize(2);
        assertThat(created).extracting(PolicyEvaluation::getPolicyID).containsExactly(policyID, otherPolicyID);
        assertThat(created).extracting(PolicyEvaluation::getResult)
                .containsExactly(EvaluationResult.ALLOW, EvaluationResult.DENY);
        assertThat(created.get(1).getErrors()).containsEntry("message", "boom");
        assertThat(created).allSatisfy(e -> assertThat(e.getId()).isNotNull());
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    void createPolicyEvaluationsWithEmptyOrNullListIsNoop() {
        assertThat(policyEvaluationDAO.createPolicyEvaluations(List.of())).isEmpty();
        assertThat(policyEvaluationDAO.createPolicyEvaluations(null)).isEmpty();
        assertThat(rowCount()).isZero();
    }

    @Test
    void createPolicyEvaluationsIsAtomic() {
        assertThatThrownBy(() -> policyEvaluationDAO.createPolicyEvaluations(List.of(
                evaluation("req-1", "user-1", policyID).build(),
                evaluation("req-1", "user-1", 999L).build()
        ))).isInstanceOf(LibraDAOException.class)
                .hasMessage("error creating policy evaluations");

        assertThat(rowCount()).isZero();
    }

    @Test
    void getPolicyEvaluationByID() {
        PolicyEvaluation created = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-1", "user-1", policyID).build());

        assertThat(policyEvaluationDAO.getPolicyEvaluationByID(created.getId())).contains(created);
        assertThat(policyEvaluationDAO.getPolicyEvaluationByID(created.getId() + 1)).isEmpty();
    }

    @Test
    void getPolicyEvaluationsByRequestIDOldestFirst() {
        List<PolicyEvaluation> req1 = policyEvaluationDAO.createPolicyEvaluations(List.of(
                evaluation("req-1", "user-1", policyID).build(),
                evaluation("req-1", "user-1", otherPolicyID).build()
        ));
        policyEvaluationDAO.createPolicyEvaluation(evaluation("req-2", "user-1", policyID).build());

        assertThat(policyEvaluationDAO.getPolicyEvaluationsByRequestID("req-1")).containsExactlyElementsOf(req1);
        assertThat(policyEvaluationDAO.getPolicyEvaluationsByRequestID("missing")).isEmpty();
    }

    @Test
    void getPolicyEvaluationsByEntityIDNewestFirst() {
        PolicyEvaluation first = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-1", "user-1", policyID).build());
        PolicyEvaluation second = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-2", "user-1", policyID).build());
        policyEvaluationDAO.createPolicyEvaluation(evaluation("req-3", "user-2", otherPolicyID).build());

        assertThat(policyEvaluationDAO.getPolicyEvaluationsByEntityID("user-1")).containsExactly(second, first);
    }

    @Test
    void getPolicyEvaluationsByPolicyIDNewestFirst() {
        PolicyEvaluation first = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-1", "user-1", policyID).build());
        PolicyEvaluation second = policyEvaluationDAO.createPolicyEvaluation(evaluation("req-2", "user-2", policyID).build());
        policyEvaluationDAO.createPolicyEvaluation(evaluation("req-3", "user-1", otherPolicyID).build());

        assertThat(policyEvaluationDAO.getPolicyEvaluationsByPolicyID(policyID)).containsExactly(second, first);
    }

    @Test
    void exposesNoUpdateOrDeleteOperations() {
        List<String> mutating = Arrays.stream(PolicyEvaluationDAO.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .filter(name -> !name.startsWith("create") && !name.startsWith("get"))
                .toList();

        assertThat(mutating).as("policy_evaluations is append only").isEmpty();
    }

}
