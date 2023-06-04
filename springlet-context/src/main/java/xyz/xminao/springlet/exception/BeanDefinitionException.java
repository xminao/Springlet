package xyz.xminao.springlet.exception;

import xyz.xminao.springlet.context.BeanDefinition;

public class BeanDefinitionException extends BeansException{
    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
