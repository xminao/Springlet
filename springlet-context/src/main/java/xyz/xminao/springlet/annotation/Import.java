package xyz.xminao.springlet.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {
    // 要导入的类列表
    Class<?>[] value();
}
