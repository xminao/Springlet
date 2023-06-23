package xyz.xminao.springlet.jdbc;

import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *  处理来自ResultSet中的每一行数据，将来自数据库中的数据映射成领域对象。
 *  作为函数参数传递。
 */
@FunctionalInterface
public interface RowMapper<T> {
    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
