package xyz.xminao.springlet.context;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * ApplicationContext的子接口，支持运行时修改应用程序上下文配置
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

    /**
     * 根据type查找符合的BeanDefinition
     */
    List<BeanDefinition> findBeanDefinitions(Class<?> type);

    /**
     * 根据Type查找某个BeanDefinition，如果不存在返回Null，如果存在多个返回@Primary标注的一个，
     * 如果存在多个@Primary标注的，或没有@Primary标注但找到多个，抛出NoUniqueBeanDefinitionException
     */
    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type);

    /**
     * 根据Name查找某个BeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name);

    /**
     * 根据Name和Type查找某个BeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(String name, Class<?> requiredType);

    /**
     * 启动时提前创建一个Bean实例放入单例缓存，以便后续使用
     */
    Object createBeanAsEarlySingleton(BeanDefinition def);
}
