package xyz.xminao.springlet.exception;

import xyz.xminao.springlet.context.BeanDefinition;

public class BeansException extends NestedRuntimeException {
    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }

}
