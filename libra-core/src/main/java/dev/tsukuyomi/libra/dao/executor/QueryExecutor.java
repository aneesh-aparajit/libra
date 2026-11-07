package dev.tsukuyomi.libra.dao.executor;

import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

public interface QueryExecutor {

    Integer update(String sql, Object... params);

    <T> List<T> query(String sql, RowMapper<T> rm);

    <T> List<T> query(String sql, RowMapper<T> rm, Object... params);

    <T> List<T> queryWithNamedParameters(String sql, RowMapper<T> rm, Map<String, ?> params);

}
