package xyz.xminao.springlet.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PropertyResolver {
    Logger logger = LoggerFactory.getLogger(getClass());
    // 内部配置项
    Map<String, String> properties = new HashMap<>();

    public PropertyResolver(Properties props) {
        // 存入环境变量
        this.properties.putAll(System.getenv());
        // 存入传进来的Properties
        Set<String> names = props.stringPropertyNames();
        for (String name : names) {
            this.properties.put(name, props.getProperty(name));
        }
        if (logger.isDebugEnabled()) {
            List<String> keys = new ArrayList<>(this.properties.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                logger.debug("PropertiesResolver: {} = {}", key, this.properties.get(key));
            }
        }

    }
}
