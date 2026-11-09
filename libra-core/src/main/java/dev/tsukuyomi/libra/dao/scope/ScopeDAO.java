package dev.tsukuyomi.libra.dao.scope;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import dev.tsukuyomi.libra.dao.executor.QueryExecutor;
import dev.tsukuyomi.libra.domain.entity.Scope;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static dev.tsukuyomi.libra.utils.Constants.ScopeDAOConstants.ID;
import static dev.tsukuyomi.libra.utils.Constants.ScopeDAOConstants.SCOPE_NAME;
import static dev.tsukuyomi.libra.utils.Constants.ScopeDAOConstants.TABLE_NAME;
import static dev.tsukuyomi.libra.utils.Constants.ScopeDAOConstants.TENANT_ID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ScopeDAO extends BaseDAO {

    private static final String CREATE_SCOPE_SQL = String.format(
            "INSERT INTO %s(%s, %s) VALUES(?, ?) RETURNING *",
            TABLE_NAME,
            TENANT_ID, SCOPE_NAME
    );

    private static final String GET_SCOPES_BY_TENANT_ID = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, TENANT_ID
    );

    private static final String GET_SCOPES_BY_ID = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, ID
    );

    private static final String GET_SCOPE_BY_ID_AND_SCOPE_NAME = String.format(
            "SELECT * FROM %s WHERE %s = ? AND %s = ?",
            TABLE_NAME, ID, SCOPE_NAME
    );

    private final QueryExecutor queryExecutor;
    private final ScopeRowMapper rowMapper;

    public Scope createScope(Scope scope) {
        try {
            Object[] params = new Object[]{
                    scope.getTenantID(),
                    scope.getScopeName()
            };
            List<Scope> scopes = queryExecutor.query(CREATE_SCOPE_SQL, rowMapper, params);
            return filterQueryObjects(scopes);
        } catch (Exception ex) {
            log.error("error creating scope", ex);
            throw new LibraDAOException("error creating scope", ex);
        }
    }

    public Optional<Scope> getScopeByIDAndScopeName(Long id, String scopeName) {
        try {
            Object[] params = new Object[]{
                    id, scopeName
            };
            List<Scope> scopes = queryExecutor.query(GET_SCOPE_BY_ID_AND_SCOPE_NAME, rowMapper, params);
            if (scopes.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(scopes));
        } catch (Exception ex) {
            log.error("error getting scope", ex);
            throw new LibraDAOException("error getting scope", ex);
        }
    }

    public List<Scope> getScopesByTenantID(Long tenantID) {
        try {
            Object[] params = new Object[]{tenantID};
            return queryExecutor.query(GET_SCOPES_BY_TENANT_ID, rowMapper, params);
        } catch (Exception ex) {
            log.error("error getting scopes", ex);
            throw new LibraDAOException("error getting scopes", ex);
        }
    }

    public Optional<Scope> getScopeByID(Long id) {
        try {
            Object[] params = new Object[]{id};
            List<Scope> scopes = queryExecutor.query(GET_SCOPES_BY_ID, rowMapper, params);
            if (scopes.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(scopes));
        } catch (Exception ex) {
            log.error("error getting scope", ex);
            throw new LibraDAOException("error getting scope", ex);
        }
    }

}
