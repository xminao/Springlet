package xyz.xminao.springlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xminao.springlet.io.ResourceResolver;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, URISyntaxException {
        //System.out.println(System.getProperty("java.class.path"));
//        String basePackagePath = "xyz/xminao/springlet";
//        ClassLoader cl = Thread.currentThread().getContextClassLoader();
//        Enumeration<URL> resources = cl.getResources(basePackagePath);
//        while (resources.hasMoreElements()) {
//            URL url = resources.nextElement();
//            URI uri = url.toURI();
//            String uriStr = uri.toString();
//            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
//            System.out.println(uriStr);
//            System.out.println(uriBaseStr);
//        }
        ResourceResolver rr = new ResourceResolver("xyz.xminao.springlet");
        List<String> classList = rr.scan(res -> {
            String name = res.name(); // 资源名称"org/example/Hello.class"
            if (name.endsWith(".class")) { // 如果以.class结尾
                // 把"org/example/Hello.class"变为"org.example.Hello":
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            // 否则返回null表示不是有效的Class Name:
            return null;
        });
        System.out.println(classList.toString());
    }
}
