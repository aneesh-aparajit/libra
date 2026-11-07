package dev.tsukuyomi.libra.dao.policy;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import dev.tsukuyomi.libra.dao.executor.QueryExecutor;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.ALL_ENTITIES;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.APPLIED_JS;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.CODE;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.ENTITY_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.EVALUATE_JS;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.SCOPE_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.STATUS;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.TABLE_NAME;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.TENANT_ID;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.VALID_FROM;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.VALID_TILL;
import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.VERSION;

@Repository
@RequiredArgsConstructor
public class PolicyDAO extends BaseDAO {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDAO.class);

    private static final long INITIAL_VERSION = 1L;

    private static final String CREATE_POLICY_SQL = String.format(
            "INSERT INTO %s(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *",
            TABLE_NAME,
            TENANT_ID, SCOPE_ID, ENTITY_ID, CODE, APPLIED_JS, EVALUATE_JS,
            STATUS, VALID_FROM, VALID_TILL, VERSION
    );

    private static final String GET_POLICY_BY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, ID
    );

    private static final String GET_POLICIES_BY_ENTITY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, ENTITY_ID
    );

    private static final String GET_POLICIES_BY_ENTITY_ID_AND_SCOPE = String.format(
            "SELECT * FROM %s WHERE %s = ? AND %s = ?",
            TABLE_NAME, ENTITY_ID, SCOPE_ID
    );

    private static final String GET_POLICIES_BY_TENANT_ID_AND_SCOPE = String.format(
            "SELECT * FROM %s WHERE %s = ? AND %s = ?",
            TABLE_NAME, TENANT_ID, SCOPE_ID
    );

    private static final String GET_APPLICABLE_POLICIES = String.format(
            "SELECT * FROM %s WHERE %s = ? AND %s = ? AND %s IN (?, ?) AND %s = ? AND %s <= ? AND (%s IS NULL OR %s > ?)",
            TABLE_NAME, TENANT_ID, SCOPE_ID, ENTITY_ID, STATUS, VALID_FROM, VALID_TILL, VALID_TILL
    );

    private static final String UPDATE_POLICY_SQL = String.format(
            "UPDATE %s SET %s = ?, %s = ?, %s = ?, %s = ?, %s = ?, %s = %s + 1 WHERE %s = ? AND %s = ? RETURNING *",
            TABLE_NAME,
            APPLIED_JS, EVALUATE_JS, STATUS, VALID_FROM, VALID_TILL,
            VERSION, VERSION,
            ID, VERSION
    );

    private static final String UPDATE_POLICY_STATUS_SQL = String.format(
            "UPDATE %s SET %s = ?, %s = %s + 1 WHERE %s = ? RETURNING *",
            TABLE_NAME, STATUS, VERSION, VERSION, ID
    );

    private static final String EXPIRE_POLICIES_SQL = String.format(
            "UPDATE %s SET %s = ?, %s = %s + 1 WHERE %s = ? AND %s IS NOT NULL AND %s <= ?",
            TABLE_NAME, STATUS, VERSION, VERSION, STATUS, VALID_TILL, VALID_TILL
    );

    private final QueryExecutor queryExecutor;
    private final PolicyRowMapper rowMapper;

    public Policy createPolicy(Policy policy) {
        try {
            Object[] params = new Object[]{
                    policy.getTenantID(),
                    policy.getScopeID(),
                    policy.getEntityID(),
                    policy.getCode(),
                    policy.getAppliedJs(),
                    policy.getEvaluatedJs(),
                    statusName(policy.getStatus()),
                    policy.getValidFrom(),
                    policy.getValidTo(),
                    Objects.requireNonNullElse(policy.getVersion(), INITIAL_VERSION)
            };
            List<Policy> policies = queryExecutor.query(CREATE_POLICY_SQL, rowMapper, params);
            return filterQueryObjects(policies);
        } catch (Exception ex) {
            logger.error("error creating policy", ex);
            throw new LibraDAOException("error creating policy", ex);
        }
    }

    public Optional<Policy> getPolicyByID(Long id) {
        try {
            Object[] params = new Object[]{id};
            List<Policy> policies = queryExecutor.query(GET_POLICY_BY_ID, rowMapper, params);
            if (policies.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(policies));
        } catch (Exception ex) {
            logger.error("error getting policy by ID", ex);
            throw new LibraDAOException("error getting policy by ID", ex);
        }
    }

    public List<Policy> getPoliciesByEntityID(String entityID) {
        try {
            Object[] params = new Object[]{entityID};
            return queryExecutor.query(GET_POLICIES_BY_ENTITY_ID, rowMapper, params);
        } catch (Exception ex) {
            logger.error("error getting policies by entity ID", ex);
            throw new LibraDAOException("error getting policies by entity ID", ex);
        }
    }

    public List<Policy> getPoliciesByEntityIDAndScope(String entityID, Long scopeID) {
        try {
            Object[] params = new Object[]{entityID, scopeID};
            return queryExecutor.query(GET_POLICIES_BY_ENTITY_ID_AND_SCOPE, rowMapper, params);
        } catch (Exception ex) {
            logger.error("error getting policies by entity ID and scope", ex);
            throw new LibraDAOException("error getting policies by entity ID and scope", ex);
        }
    }

    public List<Policy> getPoliciesByTenantIDAndScope(Long tenantID, Long scopeID) {
        try {
            Object[] params = new Object[]{tenantID, scopeID};
            return queryExecutor.query(GET_POLICIES_BY_TENANT_ID_AND_SCOPE, rowMapper, params);
        } catch (Exception ex) {
            logger.error("error getting policies by tenant ID and scope", ex);
            throw new LibraDAOException("error getting policies by tenant ID and scope", ex);
        }
    }

    public List<Policy> getApplicablePolicies(Long tenantID, Long scopeID, String entityID, Long atEpochMillis) {
        try {
            Object[] params = new Object[]{
                    tenantID,
                    scopeID,
                    ALL_ENTITIES, entityID,
                    PolicyStatus.ACTIVE.name(),
                    atEpochMillis,
                    atEpochMillis
            };
            return queryExecutor.query(GET_APPLICABLE_POLICIES, rowMapper, params);
        } catch (Exception ex) {
            logger.error("error getting applicable policies", ex);
            throw new LibraDAOException("error getting applicable policies", ex);
        }
    }

    public Optional<Policy> updatePolicy(Policy policy) {
        try {
            Object[] params = new Object[]{
                    policy.getAppliedJs(),
                    policy.getEvaluatedJs(),
                    statusName(policy.getStatus()),
                    policy.getValidFrom(),
                    policy.getValidTo(),
                    policy.getId(),
                    policy.getVersion()
            };
            List<Policy> policies = queryExecutor.query(UPDATE_POLICY_SQL, rowMapper, params);
            if (policies.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(policies));
        } catch (Exception ex) {
            logger.error("error updating policy", ex);
            throw new LibraDAOException("error updating policy", ex);
        }
    }

    public Optional<Policy> updatePolicyStatus(Long id, PolicyStatus status) {
        try {
            Object[] params = new Object[]{statusName(status), id};
            List<Policy> policies = queryExecutor.query(UPDATE_POLICY_STATUS_SQL, rowMapper, params);
            if (policies.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(policies));
        } catch (Exception ex) {
            logger.error("error updating policy status", ex);
            throw new LibraDAOException("error updating policy status", ex);
        }
    }

    public Integer expirePolicies(Long atEpochMillis) {
        try {
            return queryExecutor.update(
                    EXPIRE_POLICIES_SQL,
                    PolicyStatus.EXPIRED.name(),
                    PolicyStatus.ACTIVE.name(),
                    atEpochMillis
            );
        } catch (Exception ex) {
            logger.error("error expiring policies", ex);
            throw new LibraDAOException("error expiring policies", ex);
        }
    }

    private static String statusName(PolicyStatus status) {
        return Objects.isNull(status) ? null : status.name();
    }

}
