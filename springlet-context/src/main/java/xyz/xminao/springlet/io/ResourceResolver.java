package xyz.xminao.springlet.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * 在Classpath下搜索所有.class文件，包括子包中的文件
 * Web应用的类加载器是Servlet容器提供的，不在默认的Classpath搜索，而是在/WEB-INF/classes和目录/WEB-INF/lib中搜索
 *
 */
public class ResourceResolver {

    // 日志
    Logger logger = LoggerFactory.getLogger(getClass());

    // 扫描的包
    // eg: xyz.xminao
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    // 扫描出Classpath下的所有文件
    public <R> List<R> scan(Function<Resource, R> mapper) {
        String basePackagePath = this.basePackage.replace(".", "/");
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, basePackagePath, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // 对包路径进行扫描
    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        logger.atDebug().log("scan path: {}", path);
        // 通过ClassLoader获取URL列表
        Enumeration<URL> en = getContextClassLoader().getResources(path);
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uri.toString());
            logger.atDebug().log("uriStr" + uriStr);
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            logger.atDebug().log("uriBaseStr:" + uriBaseStr);
            if (uriBaseStr.startsWith("file:")) {
                // 在目录中搜索
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                // 在jar包中搜索
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    /**
     * 获取类加载器，用于扫描出ClassPath的所有文件
     * 首先从Thrad.getContextClassLoader获取，因为Web应用的ClassLoader不是JVM提供的基于ClassPath的
     * 而是Servlet容器提供的ClassLoader，不在默认的ClassPath中寻找，会在/WEB-INF/classes和/WEB-INF/lib
     * 中寻找
     */
    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        // 先判断是不是Servlet提供的ClassLoader
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            // 不是Servlet提供的，就是JVM基于Classpath提供的
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
    }

    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    /**
     * 删除字符串开头的斜线
     */
    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * 删除字符串末尾的斜线
     */
    String removeTrailingSlash(String s) {
        // Java字符串中\是转义符，要表示字符\要用\\
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
