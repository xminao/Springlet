package xyz.xminao.springlet.jdbc.tx;

import java.sql.Connection;

/**
 * 表示当前事务状态
 */
public class TransactionStatus {
    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
