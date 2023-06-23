package xyz.xminao.springlet.jdbc.tx;

import xyz.xminao.springlet.annotation.Transactional;
import xyz.xminao.springlet.aop.AnnotationProxyBeanPostProcessor;

public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
