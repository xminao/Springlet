package xyz.xminao.springlet.utils;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.Component;
import xyz.xminao.springlet.exception.BeanDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClassUtils {
    /**
     * 递归查找指定注解，存在多个目标则抛出异常
     *
     * @A
     * public class Hello()
     *  或，也即是Component和@Service @Controller的关系
     * @A
     * public @interface B
     *
     * @B
     * public class Hello()
     * @param target 要查找的目标类
     * @param annoClass 要查找的注解类型
     */
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annoClass) {
        // 先直接获取类上的注解，Class.getAnnotation获取类上的
        A a = target.getAnnotation(annoClass);
        // 递归遍历所有注解
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!annoType.getPackageName().equals("java.lang.annotation")) {
                // 判断是否是java.lang.annotation，即官方的注解，不是就递归查找该注解类型上有没有annoClass注解
                A found = findAnnotation(annoType, annoClass);
                if (found != null) {
                    if (a != null) {
                        // 如果存在多个目标注解，包括递归的，就抛出查找到重复注解的异常
                        throw new BeanDefinitionException("Duplicate @" + annoClass.getSimpleName() + " found on class " + target.getSimpleName());
                    }
                    a = found;
                }
            }
        }
        return a;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass) {
        for (Annotation anno : annos) {
            if (annoClass.isInstance(anno)) {
                return (A) anno;
            }
        }
        return null;
    }

    /**
     * 获取根据@Bean工厂方法创建的Bean名称，
     * 如果Bean指定了value，使用value值，否则使用被注解的方法名
     *
     * @Bean
     * Hello createHello() {}
     */
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    /**
     * get bean name by:
     *
     * @Component
     * public class Hello {}
     *
     * 如果类上存在 @Component 注解，则使用该注解的值作为 bean 名称，
     * 否则继续在其他注解中查找 @Component 注解。则使用类名作为 bean 名称。
     */
    public static String getBeanName(Class<?> clazz) {
        String name = "";
        // 查找Component
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            // @Component 存在
            name = component.value(); // Component的value
        } else {
            // 没找到@Component，继续在其他类注解中查找@Component
            for (Annotation anno : clazz.getAnnotations()) {
                if (findAnnotation(anno.annotationType(), Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                        throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }

        if (name.isEmpty()) {
            // default name: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    /**
     * get non-arg method by @PostConstruct or @PreDestroy , not search in supre class.
     *
     * @PostConstruct
     * void init() {}
     */
    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {
        // try get declared method
        List<Method> ms = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(annoClass))
                .map(m -> {
                    if (m.getParameterCount() != 0) {
                        throw new BeanDefinitionException(String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
                    }
                    return m;
                }).toList();
        if (ms.isEmpty()) {
            return null;
        }
        if (ms.size() == 1) {
            return ms.get(0);
        }
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }

    /**
     * Get non-arg method by method name. Not search in super class.
     */
    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
