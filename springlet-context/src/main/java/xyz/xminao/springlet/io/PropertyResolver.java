package xyz.xminao.springlet.io;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 配置注入支持
 *
 * 支持类型转换：
 * 值注入@Value支持String, boolean, int, Long等基本类型和包装类型，以及Data等.
 *
 * 支持三种查询方式：
 * 1. 按配置的key查询，如getProperty("jdbc.username").
 * 2. 按 ${jdbc.username} 格式查询，常用于 @Value("${jdbc.username}") 注入.
 * 3. 带默认值，如 ${jdbc.username:root} 格式查询，并支持嵌套结构，例如 ${jdbc.username:${cloud.name:root}}，
 *    先查询jdbc.username，没有找到再查询cloud.name，还没找到就返回默认值root.
 */

public class PropertyResolver {
    Logger logger = LoggerFactory.getLogger(getClass());
    // 内部配置项
    Map<String, String> properties = new HashMap<>();
    // 存储class -> Function
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入传进来的Properties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertiesResolver: {} = {}", key, this.properties.get(key));
            }
        }

        // 注册 converters
        converters.put(String.class, s -> s);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);

        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);

        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);

        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);

        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);

        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);

    }

    // 按Key查询配置项
    @Nullable
    public String getProperty(String key) {
        // 解析${abc.xyz:defaultValue}
        PropertyExpr keyExpr = parsePropertyExpr(key);
        if (keyExpr != null) {
            // 如果是${...}表达式
            if (keyExpr.defaultValue() != null) {
                // 带默认值查询
                return getProperty(keyExpr.key(), keyExpr.defaultValue());
            } else {
                // 不带默认值查询
                return getRequiredProperty(keyExpr.key());
            }
        }

        // 普通key查询
        String value = this.properties.get(key);
        if (value != null) {
            return parseValue(value);
        }
        return null;
    }

    /**
     * 带默认值查询配置项值，可能存在嵌套表达式的情况，需要解析Value
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null ? parseValue(defaultValue) : value;
    }

    // 支持类型转换
    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(targetType, value);
    }

    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return convert(targetType, value);
    }

    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    public <T> T getRequiredProperty(String key, Class<T> targetType) {
        T value = getProperty(key, targetType);
        return Objects.requireNonNull(value, "Property '" + key + "' not found.");
    }

    // 解析value,支持嵌套key，如 ${app.title:${APP_NAME:XXX}}
    String parseValue(String value) {
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null) {
            // 如果value不是${...}表达式
            return value;
        }
        // 如果是${...}表达式
        if (expr.defaultValue() != null) {
            return getProperty(expr.key(), expr.defaultValue());
        } else {
            return getRequiredProperty(expr.key());
        }
    }

    // 解析key，如${abc.xyz:defaultValue}
    PropertyExpr parsePropertyExpr(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            // 是否存在defaultValue
            int n = key.indexOf(":");
            if (n == (-1)) {
                // 如果没有defaultValue，直接返回${...}中间的内容
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k, null);
            } else {
                // 如果defaultValue
                String k = key.substring(2, n);
                String v = key.substring(n + 1, key.length() - 1);
                return new PropertyExpr(k, v);
            }
        }
        return null;
    }

    // 使用函数式接口将String转换为别的类型
    @SuppressWarnings("unchecked")
    <T> T convert(Class<?> clazz, String value) {
        Function<String, Object> fn = this.converters.get(clazz);
        if (fn == null) {
            throw new IllegalArgumentException("Unsupport value type: " + clazz.getName());
        }
        return (T) fn.apply(value);
    }
}

// 保存解析后的配置项
record PropertyExpr(String key, String defaultValue) {}
