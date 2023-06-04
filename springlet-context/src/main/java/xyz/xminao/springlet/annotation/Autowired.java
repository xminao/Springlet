package xyz.xminao.springlet.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    /**
     * is required
     */
    boolean value() default true;

    /**
     * bean name if set
     */
    String name() default  "";
}
