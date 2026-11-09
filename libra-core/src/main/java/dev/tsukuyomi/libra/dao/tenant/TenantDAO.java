package dev.tsukuyomi.libra.dao.tenant;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import dev.tsukuyomi.libra.dao.executor.QueryExecutor;
import dev.tsukuyomi.libra.domain.entity.Tenant;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static dev.tsukuyomi.libra.utils.Constants.TenantDAOConstants.ID;
import static dev.tsukuyomi.libra.utils.Constants.TenantDAOConstants.TABLE_NAME;
import static dev.tsukuyomi.libra.utils.Constants.TenantDAOConstants.TENANT_CODE;
import static dev.tsukuyomi.libra.utils.Constants.TenantDAOConstants.TENANT_NAME;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TenantDAO extends BaseDAO {

    private static final String CREATE_TENANT_SQL = String.format(
            "INSERT INTO %s(%s, %s) VALUES(?, ?) RETURNING *",
            TABLE_NAME,
            TENANT_CODE, TENANT_NAME
    );

    private static final String GET_TENANT_BY_ID_SQL = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, ID
    );

    private static final String GET_TENANT_BY_TENANT_CODE_SQL = String.format(
            "SELECT * FROM %s WHERE %s = ?",
            TABLE_NAME, TENANT_CODE
    );

    private final QueryExecutor queryExecutor;
    private final TenantRowMapper rowMapper;

    public Tenant createTenant(Tenant tenant) {
        try {
            Object[] params = new Object[]{
                    tenant.getTenantCode(),
                    tenant.getTenantName(),
            };
            List<Tenant> tenants = queryExecutor.query(CREATE_TENANT_SQL, rowMapper, params);
            return filterQueryObjects(tenants);
        } catch (Exception ex) {
            log.error("error creating tenant", ex);
            throw new LibraDAOException("error creating tenant", ex);
        }
    }

    public Optional<Tenant> getTenantByID(Long id) {
        try {
            Object[] params = new Object[]{id};
            List<Tenant> tenants = queryExecutor.query(GET_TENANT_BY_ID_SQL, rowMapper, params);
            if (tenants.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(tenants));
        } catch (Exception ex) {
            log.error("error getting tenant by ID", ex);
            throw new LibraDAOException("error getting tenant by ID", ex);
        }
    }

    public Optional<Tenant> getTenantByCode(String tenantCode) {
        try {
            Object[] params = new Object[]{tenantCode};
            List<Tenant> tenants = queryExecutor.query(GET_TENANT_BY_TENANT_CODE_SQL, rowMapper, params);
            if (tenants.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(filterQueryObjects(tenants));
        } catch (Exception ex) {
            log.error("error getting tenant by code", ex);
            throw new LibraDAOException("error getting tenant by code", ex);
        }
    }

}
