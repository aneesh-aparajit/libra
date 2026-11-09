package dev.tsukuyomi.libra.dao.policy_evaluation;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import dev.tsukuyomi.libra.dao.executor.QueryExecutor;
import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.ENTITY_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.ERRORS;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.INPUT_ATTRS;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.POLICY_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.REQUEST_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.RESULT;
import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.TABLE_NAME;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getJsonPGObject;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PolicyEvaluationDAO extends BaseDAO {

    private static final String INSERT_PREFIX = String.format(
            "INSERT INTO %s(%s, %s, %s, %s, %s, %s) VALUES",
            TABLE_NAME,
            REQUEST_ID, ENTITY_ID, POLICY_ID, INPUT_ATTRS, RESULT, ERRORS
    );

    private static final String VALUES_TUPLE = "(?, ?, ?, ?, ?, ?)";

    private static final String RETURNING = " RETURNING *";

    private static final String CREATE_POLICY_EVALUATION_SQL = INSERT_PREFIX + " " + VALUES_TUPLE + RETURNING;

    private static final String GET_POLICY_EVALUATION_BY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, ID
    );

    private static final String GET_POLICY_EVALUATIONS_BY_REQUEST_ID = String.format(
            "SELECT * FROM %s WHERE %s = ? ORDER BY %s",
            TABLE_NAME, REQUEST_ID, ID
    );

    private static final String GET_POLICY_EVALUATIONS_BY_ENTITY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ? ORDER BY %s DESC",
            TABLE_NAME, ENTITY_ID, ID
    );

    private static final String GET_POLICY_EVALUATIONS_BY_POLICY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ? ORDER BY %s DESC",
            TABLE_NAME, POLICY_ID, ID
    );

    private final QueryExecutor queryExecutor;
    private final PolicyEvaluationRowMapper rowMapper;

    public PolicyEvaluation createPolicyEvaluation(PolicyEvaluation evaluation) {
        try {
            List<PolicyEvaluation> evaluations = queryExecutor.query(
                    CREATE_POLICY_EVALUATION_SQL, rowMapper, toParams(evaluation));
            return filterQueryObjects(evaluations);
        } catch (Exception ex) {
            log.error("error creating policy evaluation", ex);
            throw new LibraDAOException("error creating policy evaluation", ex);
        }
    }

    public List<PolicyEvaluation> createPolicyEvaluations(List<PolicyEvaluation> evaluations) {
        if (Objects.isNull(evaluations) || evaluations.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String sql = INSERT_PREFIX + " " + String.join(", ", Collections.nCopies(evaluations.size(), VALUES_TUPLE)) + RETURNING;
            List<Object> params = new ArrayList<>(evaluations.size() * 6);
            for (PolicyEvaluation evaluation : evaluations) {
                Collections.addAll(params, toParams(evaluation));
            }
            List<PolicyEvaluation> created = queryExecutor.query(sql, rowMapper, params.toArray());
            if (created.size() != evaluations.size()) {
                throw new LibraDAOException("incorrect size of result: expected " + evaluations.size() + ", actual: " + created.size());
            }
            return created;
        } catch (Exception ex) {
            log.error("error creating policy evaluations", ex);
            throw new LibraDAOException("error creating policy evaluations", ex);
        }
    }

    public Optional<PolicyEvaluation> getPolicyEvaluationByID(Long id) {
        try {
            Object[] params = new Object[]{id};
            List<PolicyEvaluation> evaluations = queryExecutor.query(GET_POLICY_EVALUATION_BY_ID, rowMapper, params);
            if (evaluations.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(evaluations));
        } catch (Exception ex) {
            log.error("error getting policy evaluation by ID", ex);
            throw new LibraDAOException("error getting policy evaluation by ID", ex);
        }
    }

    public List<PolicyEvaluation> getPolicyEvaluationsByRequestID(String requestID) {
        try {
            Object[] params = new Object[]{requestID};
            return queryExecutor.query(GET_POLICY_EVALUATIONS_BY_REQUEST_ID, rowMapper, params);
        } catch (Exception ex) {
            log.error("error getting policy evaluations by request ID", ex);
            throw new LibraDAOException("error getting policy evaluations by request ID", ex);
        }
    }

    public List<PolicyEvaluation> getPolicyEvaluationsByEntityID(String entityID) {
        try {
            Object[] params = new Object[]{entityID};
            return queryExecutor.query(GET_POLICY_EVALUATIONS_BY_ENTITY_ID, rowMapper, params);
        } catch (Exception ex) {
            log.error("error getting policy evaluations by entity ID", ex);
            throw new LibraDAOException("error getting policy evaluations by entity ID", ex);
        }
    }

    public List<PolicyEvaluation> getPolicyEvaluationsByPolicyID(Long policyID) {
        try {
            Object[] params = new Object[]{policyID};
            return queryExecutor.query(GET_POLICY_EVALUATIONS_BY_POLICY_ID, rowMapper, params);
        } catch (Exception ex) {
            log.error("error getting policy evaluations by policy ID", ex);
            throw new LibraDAOException("error getting policy evaluations by policy ID", ex);
        }
    }

    private static Object[] toParams(PolicyEvaluation evaluation) {
        return new Object[]{
                evaluation.getRequestID(),
                evaluation.getEntityID(),
                evaluation.getPolicyID(),
                getJsonPGObject(evaluation.getInputAttrs()),
                Objects.isNull(evaluation.getResult()) ? null : evaluation.getResult().name(),
                getJsonPGObject(evaluation.getErrors())
        };
    }

}
