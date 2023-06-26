package xyz.xminao.springlet.web.annotation;

import xyz.xminao.springlet.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RestController {
    String value() default "";
}
