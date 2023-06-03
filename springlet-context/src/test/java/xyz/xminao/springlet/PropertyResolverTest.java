package xyz.xminao.springlet;

import org.junit.Test;
import xyz.xminao.springlet.io.PropertyResolver;
import xyz.xminao.springlet.utils.YamlUtils;

import java.util.Map;
import java.util.Properties;

public class PropertyResolverTest {
    @Test
    public void propTest() {
        Properties props = new Properties();
        props.put("jdbc.username", "root");
        props.put("qcloud.username", "xminao");
        PropertyResolver pr = new PropertyResolver(props);
        System.out.println(pr.getProperty("jdbc.username"));
        System.out.println(pr.getProperty("${jdbc.username}"));
    }

    @Test
    public void yamlTest() {
        System.out.println(System.getProperty("java.class.path"));
        Map<String, Object> configs = YamlUtils.loadYamlAsPlainMap("test.yaml");
        Properties props = new Properties();
        props.putAll(configs);
        PropertyResolver pr = new PropertyResolver(props);
    }
}
