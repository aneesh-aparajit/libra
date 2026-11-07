package dev.tsukuyomi.libra.dao.scope;

import dev.tsukuyomi.libra.domain.entity.Scope;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.tsukuyomi.libra.utils.Constants.ScopeDAOConstants.*;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getLong;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getString;

@Component
public class ScopeRowMapper implements RowMapper<Scope> {

    @Override
    public Scope mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Scope.builder()
                .id(getLong(rs, ID))
                .tenantID(getLong(rs, TENANT_ID))
                .ScopeName(getString(rs, SCOPE_NAME))
                .createdAt(getLong(rs, CREATED_AT))
                .updatedAt(getLong(rs, UPDATED_AT))
                .build();
    }

}
