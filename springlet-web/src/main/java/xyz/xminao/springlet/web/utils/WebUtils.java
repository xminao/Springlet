package xyz.xminao.springlet.web.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xminao.springlet.io.PropertyResolver;
import xyz.xminao.springlet.utils.ClassPathUtils;
import xyz.xminao.springlet.utils.YamlUtils;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

public class WebUtils {

    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YAML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";

    // 获取Springlet配置解析对象
    public static PropertyResolver createPropertyResolver() {
        final Properties props = new Properties();
        // try load application.yml
        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            logger.info("加载Springlet配置：{}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if (value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                // 如果没找到yml文件，就尝试加载properties文件
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, (input) -> {
                    logger.info("加载Springlet配置：{}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
