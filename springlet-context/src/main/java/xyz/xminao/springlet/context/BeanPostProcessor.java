package xyz.xminao.springlet.context;

public interface BeanPostProcessor {
    /**
     * 创建Bean实例后调用
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * Bean实例初始化后调用
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 注入依赖时，应该使用的Bean实例
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
