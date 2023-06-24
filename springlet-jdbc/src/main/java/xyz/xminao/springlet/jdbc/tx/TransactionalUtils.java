package xyz.xminao.springlet.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

// 获取当前事务连接工具类
public class TransactionalUtils {
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
