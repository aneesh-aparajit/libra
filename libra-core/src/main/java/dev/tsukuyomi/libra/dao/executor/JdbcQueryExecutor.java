package dev.tsukuyomi.libra.dao.executor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Service
public class JdbcQueryExecutor implements QueryExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcQueryExecutor(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Integer update(String sql, Object... params) {
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rm) {
        return namedParameterJdbcTemplate.query(sql, rm);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rm, Object... params) {
        return jdbcTemplate.query(sql, rm, params);
    }

    @Override
    public <T> List<T> queryWithNamedParameters(String sql, RowMapper<T> rm, Map<String, ?> params) {
        SqlParameterSource sqlParameterSource = new MapSqlParameterSource(params);
        return namedParameterJdbcTemplate.query(sql, sqlParameterSource, rm);
    }

}
