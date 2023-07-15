package xyz.xminao.springlet.aop;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyResolver {
    final Logger logger = LoggerFactory.getLogger(getClass());

    // 用于运行期动态织入字节码，替换CGLIB
    final ByteBuddy byteBuddy = new ByteBuddy();

    // 单例模式
    private static ProxyResolver INSTANCE = null;

    public static ProxyResolver getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProxyResolver();
        }
        return INSTANCE;
    }

    public ProxyResolver() {
    }

    /**
     * 创建代理
     * @param bean 被代理的原始Bean
     * @param handler 拦截器
     * @return 代理Bean
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(T bean, InvocationHandler handler) {
        Class<?> targetClass = bean.getClass();
        logger.atDebug().log("create proxy for bean {} @{}", targetClass.getName(), Integer.toHexString(bean.hashCode()));
        Class<?> proxyClass = this.byteBuddy
                // 创建一个指定类targetClass的子类，也就是指定一个基类
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 要拦截方法的拦截条件，这里指定了所有public方法
                .method(ElementMatchers.isPublic())
                // 指定要拦截到的方法要修改成什么样子，拦截设置返回值
                .intercept(InvocationHandlerAdapter.of(
                        // 这里创建一个新的拦截器实例，实现使用Java自带的InvocationHandler作为ByteBuddy的拦截器
                        // proxy method invoke:
                        // proxy是Proxy实例
                        (proxy, method, args) -> {
                            // delegate to origin bean:
                            // 将调用转发至原始实例
                            return handler.invoke(bean, method, args);
                        }))
                // generate proxy class:
                .make()
                // 加载字节码
                .load(targetClass.getClassLoader()).getLoaded();
        Object proxy;
        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
