package xyz.xminao.springlet;

import org.junit.Test;
import xyz.xminao.springlet.io.Resource;
import xyz.xminao.springlet.io.ResourceResolver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
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

    @Test
    public void uriTest() throws IOException, URISyntaxException {
        Enumeration<URL> en = getClass().getClassLoader().getResources("");
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            System.out.println(uri);
        }

    }
}
