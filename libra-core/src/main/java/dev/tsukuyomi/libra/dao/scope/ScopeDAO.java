package dev.tsukuyomi.libra.dao.scope;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ScopeDAO extends BaseDAO {

    private final ScopeRowMapper scopeRowMapper;

}
