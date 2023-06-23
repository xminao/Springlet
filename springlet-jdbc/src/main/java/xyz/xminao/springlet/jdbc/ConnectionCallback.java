package xyz.xminao.springlet.jdbc;

import jakarta.annotation.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionCallback<T> {
    @Nullable
    T doInConnection(Connection conn) throws SQLException;
}
