package xyz.xminao.springlet.scan;

import org.junit.Test;
import xyz.xminao.springlet.annotation.ComponentScan;
import xyz.xminao.springlet.context.AnnotationConfigApplicationContext;
import xyz.xminao.springlet.io.PropertyResolver;

public class ScanApplication {
    @Test
    public void scanTest() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ScanApplication.class, null);
    }
}
