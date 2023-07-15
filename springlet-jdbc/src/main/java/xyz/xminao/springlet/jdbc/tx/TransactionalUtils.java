package xyz.xminao.springlet.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

// 获取当前事务连接工具类,用于一个开启事务的方法内部调用其他方法
public class TransactionalUtils {
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus ts = DataSourceTransactionManager.transactionStatus.get();
        return ts == null ? null : ts.connection;
    }
}
