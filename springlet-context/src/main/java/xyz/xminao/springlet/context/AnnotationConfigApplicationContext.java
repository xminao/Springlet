package xyz.xminao.springlet.context;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xminao.springlet.annotation.*;
import xyz.xminao.springlet.exception.BeanCreationException;
import xyz.xminao.springlet.exception.BeanDefinitionException;
import xyz.xminao.springlet.exception.NoUniqueBeanDefinitionException;
import xyz.xminao.springlet.io.PropertyResolver;
import xyz.xminao.springlet.io.ResourceResolver;
import xyz.xminao.springlet.utils.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Springlet仅支持注解配置 + @Component扫描方式完成容器配置
 * 每个Bean只有一个唯一标识名
 * 仅支持单例模式
 * 依赖注入支持：构造方法、setter方法注入、字段注入
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    Logger logger = LoggerFactory.getLogger(getClass());
    // Bean只有唯一一个表示名，使用Map存储
    Map<String, BeanDefinition> beans = new HashMap<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        // 扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建Bean定义
        this.beans = createBeanDefinitions(beanClassNames);
    }

    /**
     * 根据Type在容器的beans中查找若干符合的BeanDefinition，返回0或多个
     * 也就是包括type本身，其子类，实现类
     */
    @Override
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted().collect(Collectors.toList());
    }

    /**
     * 根据Type在容器的beans中查找某个BeanDefinition，如果不存在返回Null，如果存在多个返回@Primary标注的一个，
     * 如果存在多个@Primary标注的，或没有@Primary标注但找到多个，抛出NoUniqueBeanDefinitionException
     * 注：
     *      被@Primary标注的bean会在存在多个实现某接口的bean时被优先使用
     */
    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(Class<?> type) {
        // 查找容器中符合type的bean
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if (defs.isEmpty()) {
            // 如果没有符合的，返回null
            return null;
        }
        if (defs.size() == 1) {
            // 如果只有一个
            return defs.get(0);
        }
        // 有多个符合的bean，就返回@Primary标注的那个
        List<BeanDefinition> primaryDefs = defs.stream()
                .filter(BeanDefinition::isPrimary).toList();
        if (primaryDefs.size() == 1) { // @Primary 唯一
            return primaryDefs.get(0);
        }
        if (primaryDefs.isEmpty()) { // @Primary 不存在
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else { // @Primary 不唯一
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 扫描结果是指定包的所有Class全类名，以及@Import导入的Class全类名
     */
    protected Set<String> scanForClassNames(Class<?> configClass) {
        // 获取@ComponentScan即扫描注解
        // @ComponentScan支持扫描多个包，其value为数组形式
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取注解配置的package名字，未配置则默认被@ComponentScan注解的类所在包
        String[] scanPackages = (scan == null || scan.value().length == 0) ? new String[]{configClass.getPackage().getName()} : scan.value();

        // 符合的Class类名集合
        Set<String> classNameSet = new HashSet<>();
        // 依次扫描所有包
        for (String pkg : scanPackages) {
            logger.atDebug().log("scan package: {}", pkg);
            // 扫描一个包
            var rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(res -> {
                String name = res.name();
                logger.atDebug().log("name : {}", name);
                if (name.endsWith(".class")) {
                    // 去除.class后缀，替换斜线为. 得到Class全名，如 xyz.xminao.controller.UserController
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });
            // 把扫描的结果添加到Set中
            classNameSet.addAll(classList);
        }

        // 继续查找@Import(abc.class)导入的Class配置(全类名)
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                // 获取类名
                String importClassName = importConfigClass.getName();
                classNameSet.add(importClassName);
            }
        }
        return classNameSet;
    }

    /**
     * 带有@Configuration注解的Class，视为Bean的工厂，需要查找带@Bean标注的工厂方法
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        // 遍历指定类中的所有方法
        for (Method method : clazz.getMethods()) {
            // 方法是否有@Bean注解
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) { // 抽象方法
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) { // final方法
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) { // 私有方法
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                // 该工厂方法生成的 Bean 的声明类型是方法返回类型
                Class<?> beanClass = method.getReturnType();
                if (beanClass.isPrimitive()) { // 基本数据类型
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                if (beanClass == void.class || beanClass == Void.class) { // void类型
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }
                // 使用带工厂方法的beandefinition构造函数创建benadefinition
                var def = new BeanDefinition(
                        ClassUtils.getBeanName(method), beanClass,
                        factoryBeanName,
                        // 创建bean的工厂方法
                        method,
                        // @Order
                        getOrder(method),
                        // 是否有@Primary注解
                        method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null);
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);
            }
        }
    }

    /**
     * 根据扫描到的所有全类名集合创建需要容器管理的Bean，即标注了@Component或其子注解的类
     */
    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {
        // 存储BeanDefinition的Map
        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : classNameSet) { // 遍历扫描到的所有全类名
            // 获取Class
            Class<?> clazz = null;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            // 排除不能作为Bean的class，注解、枚举、接口、record
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface() || clazz.isRecord()) {
                continue;
            }

            // 是否标注了@Compnent
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            // class标注了@Component或@Component的子注释
            if (component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                // 类修饰符，排除抽象类和私有类
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }

                // 获取Bean的名字，如果@Component的value设置了直接用value，否则用类名的首字母小写new PropertyResolver())
                logger.atDebug().log("bean name class : {}", clazz.getName());
                String beanName = ClassUtils.getBeanName(clazz);
                // 使用构造方法创建BeanDefinition
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);

                // 查找是否有@Configuration，视为Bean工厂
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    // 查找Bean方法
                    scanFactoryMethods(beanName, clazz, defs);
                }

            }
        }
        return defs;
    }

    /**
     * 检查创建的Bean是否已经存在
     */
    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }

    /**
     * 获取构造方法
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (cons.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return cons[0];
    }

    /**
     * 获取类、接口、枚举类上的order
     * @Order
     * @Component
     * public class A {}
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 获取方法上的order
     * @Order
     * @Bean
     * public User getUser() {}
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    @Override
    public boolean containsBean(String name) {
        return false;
    }

    @Override
    public <T> T getBean(String name) {
        return null;
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return null;
    }

    @Override
    public <T> List<T> getBeans(Class<T> requiredType) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public BeanDefinition findBeanDefinition(String name) {
        return null;
    }

    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        return null;
    }

    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        return null;
    }
}
