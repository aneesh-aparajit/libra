package dev.tsukuyomi.libra.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            // keep integers as Long instead of Gson's default Double when the target is Object/Map
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            // don't escape <, >, =, ' as < etc. (policy code is stored as JSON)
            .disableHtmlEscaping()
            // java.time isn't reflectively accessible on modern JDKs; serialize as ISO-8601
            .registerTypeAdapter(OffsetDateTime.class, isoAdapter(OffsetDateTime::parse).nullSafe())
            .registerTypeAdapter(Instant.class, isoAdapter(Instant::parse).nullSafe())
            .create();

    private static final Gson PRETTY_GSON = GSON.newBuilder().setPrettyPrinting().create();

    private static final TypeToken<Map<String, Object>> MAP_TYPE = new TypeToken<>() {
    };

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object value) {
        try {
            return GSON.toJson(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to serialize to JSON: " + typeName(value), e);
        }
    }

    public static String toPrettyJson(Object value) {
        try {
            return PRETTY_GSON.toJson(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to serialize to JSON: " + typeName(value), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return GSON.fromJson(json, type);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON into " + type.getName(), e);
        }
    }

    public static <T> T fromJson(String json, TypeToken<T> type) {
        if (json == null) {
            return null;
        }
        try {
            return GSON.fromJson(json, type);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON into " + type.getType(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        if (json == null) {
            return null;
        }
        try {
            return (List<T>) GSON.fromJson(json, TypeToken.getParameterized(List.class, elementType));
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON into List<" + elementType.getName() + ">", e);
        }
    }

    public static Map<String, Object> toMap(String json) {
        return fromJson(json, MAP_TYPE);
    }

    public static JsonElement toJsonElement(String json) {
        if (json == null) {
            return null;
        }
        try {
            return JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Failed to parse JSON", e);
        }
    }

    public static <T> T convert(Object value, Class<T> type) {
        try {
            return GSON.fromJson(GSON.toJsonTree(value), type);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to convert " + typeName(value) + " into " + type.getName(), e);
        }
    }

    public static <T> T convert(Object value, TypeToken<T> type) {
        try {
            return GSON.fromJson(GSON.toJsonTree(value), type);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to convert " + typeName(value) + " into " + type.getType(), e);
        }
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static <T> TypeAdapter<T> isoAdapter(Function<String, T> parser) {
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() != JsonToken.STRING) {
                    throw new JsonParseException("Expected ISO-8601 string at " + in.getPath());
                }
                return parser.apply(in.nextString());
            }
        };
    }
}
