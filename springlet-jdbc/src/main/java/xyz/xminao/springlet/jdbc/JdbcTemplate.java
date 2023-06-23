package xyz.xminao.springlet.jdbc;


import xyz.xminao.springlet.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Template模式，以回调作为参数定义模板方法
 * execute(ConnectionCallback)为基础
 */
public class JdbcTemplate {

    // 注入数据源
    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        return execute(new ConnectionCallback<T>() {
            @Override
            public T doInConnection(Connection conn) throws SQLException {
                try (PreparedStatement ps = psc.createPreparedStatement(conn)) {
                    return action.doInPreparedStatement(ps);
                }
            }
        });
    }

    private PreparedStatementCreator preparedStatementCreator(String sql, Object... args) {
        return (Connection conn) -> {
            var ps = conn.prepareStatement(sql);
            bindArgs(ps, args);
            return ps;
        };
    }

    public Number queryForNumber(String sql, Object... args) throws DataAccessException {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        }
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    T t = null;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            if (t == null) {
                                t = rowMapper.mapRow(rs, rs.getRow());
                            } else {
                                throw new DataAccessException("Multiple rows found.");
                            }
                        }
                    }
                    if (t == null) {
                        throw new DataAccessException("Empty result set.");
                    }
                    return t;
                });
    }

    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(preparedStatementCreator(sql, args),
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    List<T> list = new ArrayList<>();
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rowMapper.mapRow(rs, rs.getRow()));
                        }
                    }
                    return list;
                });
    }

    public Number updateAndReturnGeneratedKey(String sql, Object... args) throws DataAccessException {
        return execute(
                // PreparedStatementCreator
                (Connection con) -> {
                    var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    bindArgs(ps, args);
                    return ps;
                },
                // PreparedStatementCallback
                (PreparedStatement ps) -> {
                    int n = ps.executeUpdate();
                    if (n == 0) {
                        throw new DataAccessException("0 rows inserted.");
                    }
                    if (n > 1) {
                        throw new DataAccessException("Multiple rows inserted.");
                    }
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        while (keys.next()) {
                            return (Number) keys.getObject(1);
                        }
                    }
                    throw new DataAccessException("Should not reach here.");
                });
    }

    public int update(String sql, Object... args) {
        return execute(preparedStatementCreator(sql, args),
                (PreparedStatement ps) -> {
                    return ps.executeUpdate();
                });
    }

    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i += 1) {
            ps.setObject(i + 1, args[i]);
        }
    }

    /**
     * 模板方法，用于数据库连接中执行指定操作
     */
    public <T> T execute(ConnectionCallback<T> action) {
        // 获取数据库连接对象
        try (Connection newConn = dataSource.getConnection()) {
            final boolean autoCommit = newConn.getAutoCommit();
            if (!autoCommit) {
                newConn.setAutoCommit(true);
            }
            // 调用回调函数完成SQL语句执行
            T result = action.doInConnection(newConn);
            if (!autoCommit) {
                newConn.setAutoCommit(false);
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }
}

class StringRowMapper implements RowMapper<String> {

    static StringRowMapper instance = new StringRowMapper();

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);
    }
}

class BooleanRowMapper implements RowMapper<Boolean> {

    static BooleanRowMapper instance = new BooleanRowMapper();

    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(1);
    }
}

class NumberRowMapper implements RowMapper<Number> {

    static NumberRowMapper instance = new NumberRowMapper();

    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}

