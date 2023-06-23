package xyz.xminao.springlet.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.Configuration;
import xyz.xminao.springlet.annotation.Value;

import javax.sql.DataSource;

@Configuration
public class JdbcConfiguration {

    // HikariCP连接池
    @Bean(destroyMethod = "close")
    DataSource dataSource(
            @Value("${springlet.datasource.url}") String url,
            @Value("${springlet.datasource.username}") String username,
            @Value("${springlet.datasource.password}") String password,
            @Value("${springlet.datasource.driver-class-name}") String driver,
            @Value("${springlet.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${springlet.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${springlet.datasource.connection-timeout:30000}") int connTimeout
    ) {
        var config = new HikariConfig();
        config.setAutoCommit(false); // 关闭自动事务
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if (driver != null) {
            config.setDriverClassName(driver);
        }
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

}
