package dev.tsukuyomi.libra.dao;

import dev.tsukuyomi.libra.dao.executor.JdbcQueryExecutor;
import dev.tsukuyomi.libra.dao.executor.QueryExecutor;
import dev.tsukuyomi.libra.dao.policy.PolicyDAO;
import dev.tsukuyomi.libra.dao.policy.PolicyRowMapper;
import dev.tsukuyomi.libra.dao.policy_evaluation.PolicyEvaluationDAO;
import dev.tsukuyomi.libra.dao.policy_evaluation.PolicyEvaluationRowMapper;
import dev.tsukuyomi.libra.dao.scope.ScopeDAO;
import dev.tsukuyomi.libra.dao.scope.ScopeRowMapper;
import dev.tsukuyomi.libra.dao.tenant.TenantDAO;
import dev.tsukuyomi.libra.dao.tenant.TenantRowMapper;
import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * runs DAOs against a real postgres (testcontainers) with db/schema.sql applied.
 * one container is shared by every DAO test class; tables are truncated before each test.
 */
public abstract class PostgresDAOTestBase {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    protected static final DataSource DATA_SOURCE;

    static {
        POSTGRES.start();
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        DATA_SOURCE = ds;
        // executed as a single statement: the script contains a dollar-quoted plpgsql body,
        // which statement splitters (ScriptUtils) would break apart on ';'
        new JdbcTemplate(DATA_SOURCE).execute(loadSchema());
    }

    private static String loadSchema() {
        try {
            return new ClassPathResource("db/schema.sql").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("could not read db/schema.sql", ex);
        }
    }

    protected final JdbcTemplate jdbc = new JdbcTemplate(DATA_SOURCE);
    protected final QueryExecutor queryExecutor = new JdbcQueryExecutor(DATA_SOURCE);

    protected final TenantDAO tenantDAO = new TenantDAO(queryExecutor, new TenantRowMapper());
    protected final ScopeDAO scopeDAO = new ScopeDAO(queryExecutor, new ScopeRowMapper());
    protected final PolicyDAO policyDAO = new PolicyDAO(queryExecutor, new PolicyRowMapper());
    protected final PolicyEvaluationDAO policyEvaluationDAO =
            new PolicyEvaluationDAO(queryExecutor, new PolicyEvaluationRowMapper());

    @BeforeEach
    void truncateTables() {
        jdbc.execute("TRUNCATE policy_evaluations, policies, scopes, tenants RESTART IDENTITY CASCADE");
    }

    // ---------- fixtures ----------

    protected Tenant newTenant() {
        String code = "t-" + UUID.randomUUID();
        return tenantDAO.createTenant(Tenant.builder().tenantCode(code).tenantName("tenant " + code).build());
    }

    protected Scope newScope(Long tenantID, String name) {
        return scopeDAO.createScope(Scope.builder().tenantID(tenantID).ScopeName(name).build());
    }

    protected Policy.PolicyBuilder policyBuilder(Long tenantID, Long scopeID, String entityID) {
        return Policy.builder()
                .tenantID(tenantID)
                .scopeID(scopeID)
                .entityID(entityID)
                .code("policy-" + entityID)
                .appliedJs("function applies(entity, ctx) { return true; }")
                .evaluatedJs("function evaluate(input, entity, ctx) { return true; }")
                .status(PolicyStatus.ACTIVE)
                .validFrom(0L);
    }

    protected Policy newPolicy(Long tenantID, Long scopeID, String entityID) {
        return policyDAO.createPolicy(policyBuilder(tenantID, scopeID, entityID).build());
    }

}
