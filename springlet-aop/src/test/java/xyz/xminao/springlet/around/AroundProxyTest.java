package xyz.xminao.springlet.around;

import org.junit.jupiter.api.Test;
import xyz.xminao.springlet.aop.ProxyResolver;

import static org.junit.jupiter.api.Assertions.*;

public class AroundProxyTest {
    @Test
    public void testAroundProxy() {
        OriginBean origin = new OriginBean();
        origin.name = "Minao";
        // 调用原始beanhello
        assertEquals("Hello, Minao.", origin.hello());

        // 创建Proxy
        OriginBean proxy = new ProxyResolver().createProxy(origin, new AroundInvocationHandler());

        // Proxy类名
        System.out.println(proxy.getClass().getName());

        // Proxy类与originbean.class
        assertNotSame(OriginBean.class, proxy.getClass());

        // proxy实例的name为null
        assertNull(proxy.name);

        //调用带@Polite的方法
        assertEquals("Hello, Minao!", proxy.hello());
        // 调用不带@Polite的方法
        assertEquals("Morning, Minao.", proxy.morning());
    }
}
