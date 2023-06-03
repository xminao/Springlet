package xyz.xminao.springlet;

import org.junit.Test;
import xyz.xminao.springlet.io.PropertyResolver;

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
}
