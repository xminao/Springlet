package xyz.xminao.springlet.context;

import jakarta.annotation.Nullable;
import xyz.xminao.springlet.exception.BeanCreationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * 用于描述Bean的各种定义信息
 */
public class BeanDefinition implements Comparable<BeanDefinition> {
    // 全局唯一 Bean Name
    private String name;

    // Bean 的声明类型
    private Class<?> beanClass;

    // Bean 实例, 只支持单例模式
    private Object instance = null;

    // 构造方法，用于自己定义的带@Component注解的Bean实例化
    private Constructor<?> constructor;

    // 工厂名
    private String factoryName;

    // 工厂方法
    private Method factoryMethod;

    // Bean 顺序
    private int order;

    // 是否主要,即是否标识了@Primary
    private boolean primary;

    // autowired and called init method
    private boolean init = false;

    // init/destroy 方法名
    private String initMethodName;

    private String destroyMethodName;

    // init/destroy 方法
    private Method initMethod;

    private Method destroyMethod;

    /**
     * 适用于自己定义的带 @Component 注解的Bean，通过构造方法创建Bean
     */
    public BeanDefinition(String beanName, Class<?> beanClass, Constructor<?> constructor, int order, boolean primary, String initMethodName,
                          String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = beanName;
        this.beanClass = beanClass;
        this.constructor = constructor;
        this.order = order;
        this.primary = primary;
        this.factoryName = null;
        this.factoryMethod = null;
        constructor.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    /**
     * 适用于第三方Bean，即@Configuration中的@Bean方法，使用工厂方法构造
     */
    public BeanDefinition(String beanName, Class<?> beanClass, String factoryName, Method factoryMethod, int order, boolean primary, String initMethodName,
                          String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.name = beanName;
        this.beanClass = beanClass;
        this.constructor = null;
        this.factoryName = factoryName;
        this.factoryMethod = factoryMethod;
        this.order = order;
        this.primary = primary;
        factoryMethod.setAccessible(true);
        setInitAndDestroyMethod(initMethodName, destroyMethodName, initMethod, destroyMethod);
    }

    private void setInitAndDestroyMethod(String initMethodName, String destroyMethodName, Method initMethod, Method destroyMethod) {
        this.initMethodName = initMethodName;
        this.destroyMethodName = destroyMethodName;
        if (initMethod != null) {
            initMethod.setAccessible(true);
        }
        if (destroyMethod != null) {
            destroyMethod.setAccessible(true);
        }
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }

    public String getName() {
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    @Nullable
    public Object getInstance() {
        return instance;
    }

    public Object getRequiredInstance() {
        if (this.instance == null) {
            throw new BeanCreationException(String.format("Instance of bean with name '%s' and type '%s' is not instantiated during current stage.",
                    this.getName(), this.getBeanClass().getName()));
        }
        return this.instance;
    }

    public void setInstance(Object instance) {
        Objects.requireNonNull(instance, "Bean instance is null");
        if (!this.beanClass.isAssignableFrom(instance.getClass())) {
            throw new BeanCreationException(String.format("Instance '%s' of Bean '%s' is not the expected type: %s", instance, instance.getClass().getName(),
                    this.beanClass.getName()));
        }
        this.instance = instance;
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return constructor;
    }

    @Nullable
    public String getFactoryName() {
        return factoryName;
    }

    @Nullable
    public Method getFactoryMethod() {
        return factoryMethod;
    }

    @Nullable
    public int getOrder() {
        return order;
    }

    @Nullable
    public String getInitMethodName() {
        return initMethodName;
    }

    @Nullable
    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    @Nullable
    public Method getInitMethod() {
        return initMethod;
    }

    @Nullable
    public Method getDestroyMethod() {
        return destroyMethod;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

    @Override
    public int compareTo(BeanDefinition def) {
        int cmp = Integer.compare(this.order, def.order);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(def.name);
    }

    @Override
    public String toString() {
        return "BeanDefinition{" +
                "name='" + name + '\'' +
                ", beanClass=" + beanClass +
                ", instance=" + instance +
                ", constructor=" + constructor +
                ", factoryName='" + factoryName + '\'' +
                ", factoryMethod=" + factoryMethod +
                ", order=" + order +
                ", primary=" + primary +
                ", init=" + init +
                ", initMethodName='" + initMethodName + '\'' +
                ", destroyMethodName='" + destroyMethodName + '\'' +
                ", initMethod=" + initMethod +
                ", destroyMethod=" + destroyMethod +
                '}';
    }

    String getCreateDetail() {
        if (this.factoryMethod != null) {
            String params = String.join(", ", Arrays.stream(this.factoryMethod.getParameterTypes()).map(t -> t.getSimpleName()).toArray(String[]::new));
            return this.factoryMethod.getDeclaringClass().getSimpleName() + "." + this.factoryMethod.getName() + "(" + params + ")";
        }
        return null;
    }
}
