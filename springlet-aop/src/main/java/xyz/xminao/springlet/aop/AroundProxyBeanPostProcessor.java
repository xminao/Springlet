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

public class AroundProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Around> {
}
