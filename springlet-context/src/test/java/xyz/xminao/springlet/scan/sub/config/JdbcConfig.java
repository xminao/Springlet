package xyz.xminao.springlet.scan.sub.config;

import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.Configuration;
import xyz.xminao.springlet.scan.sub.jdbc.JDBC;

@Configuration
public class JdbcConfig {
    @Bean
    public JDBC jdbc() {
        return new JDBC();
    }
}
