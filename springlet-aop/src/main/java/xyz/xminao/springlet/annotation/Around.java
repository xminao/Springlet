package xyz.xminao.springlet.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Around {
    // 拦截器bean的名字，用于创建代理时通过容器寻找拦截器bean实例
    String value();
}
