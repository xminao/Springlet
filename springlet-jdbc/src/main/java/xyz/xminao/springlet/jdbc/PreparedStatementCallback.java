package xyz.xminao.springlet.jdbc;

import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCallback<T> {
    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;
}
