package xyz.xminao.springlet.around;

import xyz.xminao.springlet.annotation.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 拦截器
 */
@Component
public class AroundInvocationHandler implements InvocationHandler {

    // 自定义一个拦截器，proxy为代理类的实例
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 拦截标记了@Polite的方法的返回值
        if (method.getAnnotation(Polite.class) != null) {
            // 获取原始bean的method返回值
            String ret = (String) method.invoke(proxy, args);
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!";
            }
            return ret;
        }
        return method.invoke(proxy, args);
    }
}
