package dev.tsukuyomi.libra.dao.policy_evaluation;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PolicyEvaluationDAO extends BaseDAO {

    private final PolicyEvaluationRowMapper rowMapper;

}
