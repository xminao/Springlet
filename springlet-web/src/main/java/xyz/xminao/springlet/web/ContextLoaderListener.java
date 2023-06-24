package xyz.xminao.springlet.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import xyz.xminao.springlet.context.AnnotationConfigApplicationContext;
import xyz.xminao.springlet.context.ApplicationContext;
import xyz.xminao.springlet.exception.NestedRuntimeException;
import xyz.xminao.springlet.io.PropertyResolver;
import xyz.xminao.springlet.web.utils.WebUtils;

/**
 * 实现 ServletContextListener，监听Servlet容器的启动和销毁
 * 监听到初始化事件时，完成创建IoC容器和注册DispatcherServlet
 */
public class ContextLoaderListener implements ServletContextListener {
    /**
     * Servlet启动时自动调用
     * 先获取ServletContext引用，通过getInitParameter获取servlet容器完整类名，创建IoC容器
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 获取servlet容器
        var servletContext = sce.getServletContext();
        // 读取Springlet配置文件: application.yml 或 application.properties
        var propertyResolver = WebUtils.createPropertyResolver();
        // 创建IoC容器
        // @Configuration配置类从web.xml配置读取
        var applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        // 实例化DispatcherServlet
        var dispatcherServlet = new DispatcherServlet();
        // 注册DispatcherServlet
        var dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    // 创建IoC容器
    ApplicationContext createApplicationContext(String configClassName, PropertyResolver propertyResolver) {
        if (configClassName == null || configClassName.isEmpty()) {
            throw new NestedRuntimeException("Cannot init ApplicationContext for missing init param name: configuration");
        }
        Class<?> configClass;
        try {
            configClass = Class.forName(configClassName);
        } catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': \" + configClassName");
        }
        return new AnnotationConfigApplicationContext(configClass, propertyResolver);
    }
}
