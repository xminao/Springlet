package xyz.xminao.springlet.jdbc.tx;

import xyz.xminao.springlet.annotation.Transactional;
import xyz.xminao.springlet.aop.ProxyResolver;
import xyz.xminao.springlet.context.ApplicationContextUtils;
import xyz.xminao.springlet.context.BeanDefinition;
import xyz.xminao.springlet.context.BeanPostProcessor;
import xyz.xminao.springlet.context.ConfigurableApplicationContext;
import xyz.xminao.springlet.exception.AopConfigException;
import xyz.xminao.springlet.exception.BeansException;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

// 让AOP机制生效，拦截@Transactional注解的bean，生成代理对象
public class TransactionalBeanPostProcessor implements BeanPostProcessor {

    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        Transactional anno = beanClass.getAnnotation(Transactional.class);
        if (anno != null) {
            String handlerName;
            try {
                handlerName = (String) anno.annotationType().getMethod("value").invoke(bean);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException(e);
            }
            Object proxy = createProxy(beanClass, bean, handlerName);
            originBeans.put(beanName, bean);
            return proxy;
        } else {
            return bean;
        }
    }

    Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        // 获取容器实例对象
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        BeanDefinition def = ctx.findBeanDefinition(handlerName);
        if (def == null) {
            throw new AopConfigException();
        }
        // 先获取handler的bean
        Object handlerBean = def.getInstance();
        if (handlerBean == null) {
            handlerBean = ctx.createBeanAsEarlySingleton(def);
        }
        if (handlerBean instanceof InvocationHandler handler) {
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException();
        }
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        return bean == null ? bean : origin;
    }
}
