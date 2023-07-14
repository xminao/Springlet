package xyz.xminao.springlet.context;

import java.util.List;

/**
 * 核心接口，用于表示应用程序上下文
 * 用户使用
 * 在Spring中继承自BeanFactory，二者都是Ioc容器，但是ApplicationContext启动时就加载Bean，BeanFactory用到时才加载
 * Application包含BeanFactory所有功能并进行了拓展
 */
public interface ApplicationContext extends AutoCloseable {
    /**
     * 容器中是否存在指定Name的Bean
     */
    boolean containsBean(String name);

    /**
     * 根据name返回唯一bean,未找到抛出异常NoSuchBeanDefinitionException
     *
     */
    <T> T getBean(String name);

    /**
     * 根据name和type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException，找到但type不符抛出BeanNotOfRequiredTypeException
     */
    <T> T getBean(String name, Class<T> requiredType);

    /**
     * 根据type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
     */
    <T> T getBean(Class<T> requiredType);

    /**
     * 根据type返回一组Bean，未找到返回空List
     */
    <T> List<T> getBeans(Class<T> requiredType);

    /**
     * 关闭并执行所有bean的destroy方法
     */
    void close();
}
