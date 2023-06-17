package xyz.xminao.springlet.around;

import xyz.xminao.springlet.annotation.Bean;
import xyz.xminao.springlet.annotation.ComponentScan;
import xyz.xminao.springlet.annotation.Configuration;
import xyz.xminao.springlet.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AroundApplication {
    /**
     * create BPP bean instance.
     */
    @Bean
    AroundProxyBeanPostProcessor createAroundBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
