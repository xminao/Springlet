package xyz.xminao.springlet.aop;

import xyz.xminao.springlet.annotation.Around;
import xyz.xminao.springlet.context.ApplicationContextUtils;
import xyz.xminao.springlet.context.BeanDefinition;
import xyz.xminao.springlet.context.BeanPostProcessor;
import xyz.xminao.springlet.context.ConfigurableApplicationContext;
import xyz.xminao.springlet.exception.AopConfigException;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * 检测每个Bean实例是否有@Around实例，如果有就根据注解的value查找Bean作为拦截器，创建proxy。
 * 返回前保存原始Bean的引用，后序ioc注入依赖要注入到原始bean。
 */

public class AroundProxyBeanPostProcessor implements BeanPostProcessor {

    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> beanClass = bean.getClass();
        // 检测 @Around
        Around anno = beanClass.getAnnotation(Around.class);
        if (anno != null) {
            // 存在 @Around
            String handlerName;
            try {
                handlerName = (String) anno.annotationType().getMethod("value").invoke(anno);
            } catch (ReflectiveOperationException e) {
                throw new AopConfigException();
            }
            // 找到拦截器就根据bean和拦截器名创建代理
            Object proxy = createProxy(beanClass, bean, handlerName);
            // 保存原始bean
            originBeans.put(beanName, bean);
            return proxy;
        } else {
            return bean;
        }
    }

    Object createProxy(Class<?> beanClass, Object bean, String handlerName) {
        ConfigurableApplicationContext ctx = (ConfigurableApplicationContext) ApplicationContextUtils.getRequiredApplicationContext();
        // 查找拦截器Bean
        BeanDefinition def = ctx.findBeanDefinition(handlerName);
        if (def == null) {
            throw new AopConfigException();
        }
        Object handlerBean = def.getInstance();
        if (handlerBean == null) {
            ctx.createBeanAsEarlySingleton(def);
        }
        // bean是拦截器类型
        if (handlerBean instanceof InvocationHandler handler) {
            return ProxyResolver.getInstance().createProxy(bean, handler);
        } else {
            throw new AopConfigException();
        }
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = this.originBeans.get(beanName);
        return origin != null ? origin : bean;
    }
}

