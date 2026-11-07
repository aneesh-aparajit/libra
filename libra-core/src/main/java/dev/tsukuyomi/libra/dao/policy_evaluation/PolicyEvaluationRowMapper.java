package dev.tsukuyomi.libra.dao.policy_evaluation;

import dev.tsukuyomi.libra.domain.entity.PolicyEvaluation;
import dev.tsukuyomi.libra.domain.enums.EvaluationResult;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

import static dev.tsukuyomi.libra.utils.Constants.PolicyEvaluationDAOConstants.*;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getEnum;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getLong;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getMap;
import static dev.tsukuyomi.libra.utils.JdbcUtil.getString;

@Component
public class PolicyEvaluationRowMapper implements RowMapper<PolicyEvaluation> {

    @Override
    public PolicyEvaluation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PolicyEvaluation.builder()
                .id(getLong(rs, ID))
                .requestID(getString(rs, REQUEST_ID))
                .entityID(getString(rs, ENTITY_ID))
                .policyID(getLong(rs, POLICY_ID))
                .inputAttrs(getMap(rs, INPUT_ATTRS))
                .result(getEnum(rs, RESULT, EvaluationResult.class))
                .errors(getMap(rs, ERRORS))
                .createdAt(getLong(rs, CREATED_AT))
                .updatedAt(getLong(rs, UPDATED_AT))
                .build();
    }

}
