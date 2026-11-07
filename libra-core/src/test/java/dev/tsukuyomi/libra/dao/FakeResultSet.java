package dev.tsukuyomi.libra.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * builds a mocked {@link ResultSet} for a single row, backed by a column -> value map.
 * mimics JDBC semantics: primitive getters return 0 for SQL NULL and {@code wasNull()} reports it.
 */
public final class FakeResultSet {

    private FakeResultSet() {
    }

    public static ResultSet of(Map<String, Object> row) throws SQLException {
        Map<String, Object> values = new HashMap<>(row);
        AtomicBoolean lastWasNull = new AtomicBoolean(false);
        ResultSet rs = mock(ResultSet.class);

        when(rs.getString(anyString())).thenAnswer(inv -> {
            Object value = values.get(inv.<String>getArgument(0));
            lastWasNull.set(value == null);
            return value == null ? null : value.toString();
        });
        when(rs.getLong(anyString())).thenAnswer(inv -> {
            Object value = values.get(inv.<String>getArgument(0));
            lastWasNull.set(value == null);
            return value == null ? 0L : ((Number) value).longValue();
        });
        when(rs.getInt(anyString())).thenAnswer(inv -> {
            Object value = values.get(inv.<String>getArgument(0));
            lastWasNull.set(value == null);
            return value == null ? 0 : ((Number) value).intValue();
        });
        when(rs.wasNull()).thenAnswer(inv -> lastWasNull.get());
        return rs;
    }

    /**
     * a result set whose every read fails, to check that driver errors are wrapped.
     */
    public static ResultSet failing() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        SQLException boom = new SQLException("boom");
        when(rs.getString(anyString())).thenThrow(boom);
        when(rs.getLong(anyString())).thenThrow(boom);
        when(rs.getInt(anyString())).thenThrow(boom);
        return rs;
    }

}
