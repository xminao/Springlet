package xyz.xminao.springlet.around;

import xyz.xminao.springlet.annotation.Around;
import xyz.xminao.springlet.annotation.Component;

@Component
@Around("aroundInvocationHandler")
public class OriginBean {
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
