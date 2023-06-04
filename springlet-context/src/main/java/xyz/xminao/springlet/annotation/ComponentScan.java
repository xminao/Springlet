package xyz.xminao.springlet.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ComponentScan {
    // 要扫描的包名
    String[] value() default {};
}
