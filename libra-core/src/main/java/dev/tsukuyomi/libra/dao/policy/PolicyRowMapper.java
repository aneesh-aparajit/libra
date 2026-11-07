package dev.tsukuyomi.libra.dao.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.PolicyStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.tsukuyomi.libra.utils.Constants.PolicyDAOConstants.*;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getEnum;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getLong;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getString;

@Component
public class PolicyRowMapper implements RowMapper<Policy> {

    @Override
    public Policy mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Policy.builder()
                .id(getLong(rs, ID))
                .tenantID(getLong(rs, TENANT_ID))
                .scopeID(getLong(rs, SCOPE_ID))
                .entityID(getString(rs, ENTITY_ID))
                .code(getString(rs, CODE))
                .applied_js(getString(rs, APPLIED_JS))
                .evaluated_js(getString(rs, EVALUATE_JS))
                .status(getEnum(rs, STATUS, PolicyStatus.class))
                .validFrom(getLong(rs, VALID_FROM))
                .validTo(getLong(rs, VALID_TILL))
                .version(getLong(rs, VERSION))
                .createdAt(getLong(rs, CREATED_AT))
                .updatedAt(getLong(rs, UPDATED_AT))
                .build();
    }

}
