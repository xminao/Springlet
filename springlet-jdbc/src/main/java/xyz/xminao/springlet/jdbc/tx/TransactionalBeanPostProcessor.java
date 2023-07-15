package xyz.xminao.springlet.jdbc.tx;

import xyz.xminao.springlet.annotation.Transactional;

// 让AOP机制生效，拦截@Transactional注解的bean，生成代理对象
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
