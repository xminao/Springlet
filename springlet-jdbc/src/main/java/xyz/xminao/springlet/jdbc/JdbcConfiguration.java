package xyz.xminao.springlet.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import xyz.xminao.springlet.annotation.Autowired;
import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.Configuration;
import xyz.xminao.springlet.annotation.Value;
import xyz.xminao.springlet.jdbc.tx.DataSourceTransactionManager;
import xyz.xminao.springlet.jdbc.tx.PlatformTransactionManager;
import xyz.xminao.springlet.jdbc.tx.TransactionalBeanPostProcessor;

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

    /**
     * 实现基本SQL操作
     */
    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 负责拦截 @Transactional标识的Bean的public方法，自动管理事务
     */
    @Bean
    TransactionalBeanPostProcessor transactionalBeanPostProcessor() {
        return new TransactionalBeanPostProcessor();
    }

    /**
     * 给@Transactional标识的bean创建AOP代理，拦截器使用PlatformTransactionManager
     */
    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
