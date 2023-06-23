package xyz.xminao.springlet.jdbc;


import xyz.xminao.springlet.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcTemplate {

    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        return execute((Connection conn) -> {
            try (PreparedStatement ps = psc.createPreparedStatement(conn)) {
                return action.doInPreparedStatement(ps);
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

    private void bindArgs(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i += 1) {
            ps.setObject(i + 1, args[i]);
        }
    }

    public <T> T execute(ConnectionCallback<T> action) {
        try (Connection newConn = dataSource.getConnection()) {
            final boolean autoCommit = newConn.getAutoCommit();
            if (!autoCommit) {
                newConn.setAutoCommit(true);
            }
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
