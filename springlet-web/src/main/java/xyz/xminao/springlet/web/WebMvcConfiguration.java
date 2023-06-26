package xyz.xminao.springlet.web;

import jakarta.servlet.ServletContext;
import xyz.xminao.springlet.annotation.Autowired;
import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.Configuration;
import xyz.xminao.springlet.annotation.Value;

import java.util.Objects;

/**
 * 简化配置
 * 默认创建一个ViewResolver 和 ServletContext
 */
@Configuration
public class WebMvcConfiguration {
    private static ServletContext servletContext = null;

    // 由Listener处理
    static void setServletContext(ServletContext ctx) {
        servletContext = ctx;
    }

    @Bean(initMethod = "init")
    ViewResolver viewResolver(
            @Autowired ServletContext servletContext,
            @Value("${springlet.web.freemarker.template-path:/WEB-INF/templates}") String templatePath,
            @Value("${springlet.web.freemarket.template-encoding:UTF-8}") String templateEncoding
    ) {
        return new FreeMarketViewResolver(templatePath, templateEncoding, servletContext);
    }

    @Bean
    ServletContext servletContext() {
        return Objects.requireNonNull(servletContext(), "ServletContext is not set");
    }
}
