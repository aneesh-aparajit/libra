package dev.tsukuyomi.libra.utils;

import com.google.gson.reflect.TypeToken;
import dev.tsukuyomi.libra.exeception.LibraDAOException;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class JdbcUtil {

    private static final Logger log = LoggerFactory.getLogger(JdbcUtil.class);

    private static final String JSONB = "jsonb";
    private static final TypeToken<Map<String, Object>> MAP_TYPE = new TypeToken<>() {
    };

    private JdbcUtil() {
    }

    public static String getString(ResultSet rs, String columnName) {
        return read(rs, columnName, "String", () -> rs.getString(columnName));
    }

    public static Integer getInt(ResultSet rs, String columnName) {
        return read(rs, columnName, "Integer", () -> {
            int value = rs.getInt(columnName);
            return rs.wasNull() ? null : value;
        });
    }

    public static Long getLong(ResultSet rs, String columnName) {
        return read(rs, columnName, "Long", () -> {
            long value = rs.getLong(columnName);
            return rs.wasNull() ? null : value;
        });
    }

    public static <E extends Enum<E>> E getEnum(ResultSet rs, String columnName, Class<E> enumType) {
        String value = getString(rs, columnName);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException e) {
            log.error("Unknown {} value '{}' in column '{}'", enumType.getSimpleName(), value, columnName, e);
            throw new LibraDAOException(
                    "Unknown " + enumType.getSimpleName() + " value '" + value + "' in column '" + columnName + "'", e);
        }
    }

    public static <T> T getObject(ResultSet rs, String columnName, Class<T> type) {
        return read(rs, columnName, type.getSimpleName(), () -> rs.getObject(columnName, type));
    }

    public static OffsetDateTime getOffsetDateTime(ResultSet rs, String columnName) {
        return getObject(rs, columnName, OffsetDateTime.class);
    }

    public static <T> T getJson(ResultSet rs, String columnName, Class<T> type) {
        String json = getString(rs, columnName);
        return json == null ? null : parse(columnName, type.getSimpleName(), () -> JsonUtil.fromJson(json, type));
    }

    public static <T> T getJson(ResultSet rs, String columnName, TypeToken<T> type) {
        String json = getString(rs, columnName);
        return json == null ? null : parse(columnName, type.getType().getTypeName(), () -> JsonUtil.fromJson(json, type));
    }

    public static Map<String, Object> getMap(ResultSet rs, String columnName) {
        return getJson(rs, columnName, MAP_TYPE);
    }

    public static <T> List<T> getList(ResultSet rs, String columnName, Class<T> elementType) {
        String json = getString(rs, columnName);
        return json == null ? null
                : parse(columnName, "List<" + elementType.getSimpleName() + ">", () -> JsonUtil.fromJsonList(json, elementType));
    }

    public static PGobject getJsonPGObject(Object value) {
        if (value == null) {
            return null;
        }
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType(JSONB);
            pgObject.setValue(JsonUtil.toJson(value));
            return pgObject;
        } catch (SQLException | IllegalArgumentException e) {
            log.error("Failed to build jsonb PGobject from {}", value.getClass().getName(), e);
            throw new LibraDAOException("Failed to build jsonb PGobject from " + value.getClass().getName(), e);
        }
    }

    public static boolean anyNull(ResultSet rs, String columnName) {
        return rs == null || columnName == null || columnName.isBlank();
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }

    private static <T> T read(ResultSet rs, String columnName, String targetType, SqlSupplier<T> supplier) {
        if (anyNull(rs, columnName)) {
            return null;
        }
        try {
            return supplier.get();
        } catch (SQLException e) {
            log.error("Failed to read column '{}' as {}", columnName, targetType, e);
            throw new LibraDAOException("Failed to read column '" + columnName + "' as " + targetType, e);
        }
    }

    private static <T> T parse(String columnName, String targetType, java.util.function.Supplier<T> parser) {
        try {
            return parser.get();
        } catch (IllegalArgumentException e) {
            log.error("Failed to deserialize json column '{}' into {}", columnName, targetType, e);
            throw new LibraDAOException("Failed to deserialize json column '" + columnName + "' into " + targetType, e);
        }
    }

}
