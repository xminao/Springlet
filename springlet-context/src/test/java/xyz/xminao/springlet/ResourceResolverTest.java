package xyz.xminao.springlet;

import org.junit.Test;
import xyz.xminao.springlet.io.Resource;
import xyz.xminao.springlet.io.ResourceResolver;

import java.util.List;
import java.util.function.Function;

public class ResourceResolverTest {
    @Test
    public void resourceTest() {
        ResourceResolver rr = new ResourceResolver("xyz.xminao");
        List<String> list = rr.scan(new Function<Resource, String>() {
            @Override
            public String apply(Resource resource) {
                String name = resource.name();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            }
        });
        list.forEach(System.out::println);
    }
}
