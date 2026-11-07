package dev.tsukuyomi.libra.dao.policy;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PolicyDAO extends BaseDAO {

    private final PolicyRowMapper policyRowMapper;

}
