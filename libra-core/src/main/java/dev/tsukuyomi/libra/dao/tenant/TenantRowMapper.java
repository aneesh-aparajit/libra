package dev.tsukuyomi.libra.dao.tenant;

import dev.tsukuyomi.libra.domain.entity.Tenant;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.tsukuyomi.libra.utils.Constants.TenantDAOConstants.*;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getLong;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getString;

@Component
public class TenantRowMapper implements RowMapper<Tenant> {

    @Override
    public Tenant mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        return Tenant.builder()
                .id(getLong(rs, ID))
                .tenantCode(getString(rs, TENANT_CODE))
                .tenantName(getString(rs, TENANT_NAME))
                .createdAt(getLong(rs, CREATED_AT))
                .updatedAt(getLong(rs, UPDATED_AT))
                .build();
    }

}
