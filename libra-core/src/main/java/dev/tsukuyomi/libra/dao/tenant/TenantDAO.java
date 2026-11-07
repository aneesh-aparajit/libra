package dev.tsukuyomi.libra.dao.tenant;

import dev.tsukuyomi.libra.dao.base.BaseDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TenantDAO extends BaseDAO {

    private final TenantRowMapper tenantRowMapper;

}
