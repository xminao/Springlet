package xyz.xminao.springlet.context;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.xminao.springlet.annotation.*;
import xyz.xminao.springlet.exception.*;
import xyz.xminao.springlet.io.PropertyResolver;
import xyz.xminao.springlet.io.ResourceResolver;
import xyz.xminao.springlet.utils.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
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
    PropertyResolver propertyResolver;

    // Bean只有唯一一个表示名，使用Map存储所有BeanDefinition
    // 没办法用 <Class, BeanDefinition>是因为Bean声明类型和实例类型不一定相符
    Map<String, BeanDefinition> beans;

    private Set<String> creatingBeanNames;
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;

        // 1. BeanDefinition阶段
        // 1.1 扫描获取所有Bean的Class类型
        Set<String> beanClassNames = scanForClassNames(configClass);

        // 1.2 创建Bean定义
        this.beans = createBeanDefinitions(beanClassNames);

        // 2. 创建Bean阶段
        // 创建BeanName循环检测依赖关系
        this.creatingBeanNames = new HashSet<>();

        // 创建@Configuration类型的Bean，不能通过注入创建
        this.beans.values().stream()
                // 过滤出 @Configuration
                .filter(this::isConfigurationDefinition)
                .sorted().map(def -> {
                    // 创建bean实例
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).collect(Collectors.toList());

        // 创建BeanPostProcessor类型的Bean
        List<BeanPostProcessor> processors = this.beans.values().stream()
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                .map(def -> (BeanPostProcessor) createBeanAsEarlySingleton(def)).toList();
        this.beanPostProcessors.addAll(processors);

        // 创建其他普通bean
        List<BeanDefinition> defs = this.beans.values().stream()
                // 过滤出没创建实例的beandefinition
                .filter(def -> def.getInstance() == null)
                .sorted().collect(Collectors.toList());
        defs.forEach(def -> {
            // 如果Bean未被创建,可能存在其他Bean的构造方法注入前被创建
            if (def.getInstance() == null) {
                // 创建Bean
                createBeanAsEarlySingleton(def);
            }
        });

        // 通过字段和setter方法注入依赖（属于弱依赖）
        logger.atDebug().log("beans: {}", beans.values().stream().map(BeanDefinition::getName).toList());
        this.beans.values().forEach(this::injectBean);

        // 调用init方法
        this.beans.values().forEach(this::initBean);
    }

    /**
     * 根据Type在容器的beans中查找若干符合的BeanDefinition，返回0或多个
     * 也就是包括type本身，其子类，实现类
     */
    @Override
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
//        List<BeanDefinition> defs = new ArrayList<>();
//        for (BeanDefinition def : this.beans.values()) {
//            if (type.isAssignableFrom(def.getBeanClass())) {
//                defs.add(def);
//            }
//        }
//        Collections.sort(defs);
//        return defs;
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
        // 查找符合type的BeanDefinition
        List<BeanDefinition> defs = findBeanDefinitions(type);
        // 1. 没有符合的，直接返回null
        if (defs.isEmpty()) {
            return null;
        }
        // 2. 有一个符合的，直接返回
        if (defs.size() == 1) {
            return defs.get(0);
        }
        // 3. 有多个符合的，需要借助@Primary注解
//        List<BeanDefinition> primaryDefs = new ArrayList<>();
//        for (BeanDefinition def : defs) {
//            if (def.isPrimary()) {
//                primaryDefs.add(def);
//            }
//        }
        List<BeanDefinition> primaryDefs = defs.stream()
                .filter(BeanDefinition::isPrimary).toList();

        // 3.1 有一个@Primary，直接返回
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        // 3.2 不存在@Primary，抛出异常
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("存在多个 %s 类型的Bean，但不存在 @Primary Bean定义。", type));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("存在多个 %s 类型的Bean，但不存在唯一的 @Primary Bean定义。", type));
        }
    }

    /**
     * 扫描指定包下的所有Class，返回Class全类名用于加载创建实例，包括@Import注解定义的Class类名
     * 1. 获取入口配置的@ComponenntScan注解
     * 2. 获取注解配置的basePackage，没配置就默认入口配置类所在包
     * 3. 使用ResourceResolver获取出所有指定包下的Class的全类名
     * 4. 查找@Import导入的Class配置
      */
    protected  Set<String> scanForClassNames(Class<?> configClass) {
        // 获取入口配置的@ComponentScan注解
        ComponentScan scan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        // 获取注解配置的basePackage，没配置就默认入口配置类所在包
        String[] scanPackages = (scan == null || scan.value().length == 0) ? new String[]{configClass.getPackage().getName()} : scan.value();

        // 指定包下的所有ClassNames
        Set<String> classNames = new HashSet<>();
        // 挨个扫描包
        for (String pkg : scanPackages) {
            // 使用ResourceResolver获取指定包下的所有Class全类名
            ResourceResolver rr = new ResourceResolver(pkg);
            List<String> classList = rr.scan(resource -> {
                String name = resource.name();
                if (name.endsWith(".class")) {
                    // 去除.class后缀，替换 / \ 为.
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                // 不是.class 结尾
                return null;
            });
            classNames.addAll(classList);
        }

        // 加入@Import配置的classNames
        Import importConfig = ClassUtils.findAnnotation(configClass, Import.class);
        if (importConfig != null) {
            for (Class<?> clazz : importConfig.value()) {
                String className = clazz.getName();
                classNames.add(className);
            }
        }

        // 返回扫描到的所有指定包下的class全类名集合
        return classNames;
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
                String beanName = ClassUtils.getBeanName(clazz);
                // 使用构造方法创建BeanDefinition
                // @Component注解的都是自定义的bean，直接构造方法创建，这个阶段不会创建@Bean定义的三方组件
                var def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        null, null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs, def);

                // 查找是否有@Configuration，视为Bean工厂，这时候创建@Bean标注的bean
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

    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
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

    /**
     * 通过Name查找Bean
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Name和Type查找Bean
     */
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return t;
    }

    /**
     * 通过Type查找Bean，不存在抛出异常，存在多个但缺少唯一@Primary抛出NoUnique异常
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     *  通过type查beans
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return List.of();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (var def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    /**
     * find 不存在返回null，get 不存在抛出异常
     */

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
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

    /**
     * 注入但不调用init
     * 通过BeanDefinition对Bean进行注入
     */
    void injectBean(BeanDefinition def) {
        // 获取bean实例，或被代理的原始实例,BeanPostProcessor功能要用
        // 不从BeanDefinition中获取实例，因为可能是proxy对象，而是获取原始bean
        final Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * 调用init方法
     */
    void initBean(BeanDefinition def) {
        // 获取bean实例，或被代理的原始实例
        final Object beanInstance = getProxiedInstance(def);

        // 调用init方法
        callMethod(beanInstance, def.getInitMethod(), def.getInitMethodName());

        // 调用BeanPostProcessor
    }

    /**
     * 在当前Bean实例以及父类进行字段和方法注入
     */
    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws InvocationTargetException, IllegalAccessException {
        // 在当前类遍历查找Field和Method并注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // 在父类查找Field和Method并注入，利用递归
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, bean);
        }
    }

    /**
     * 注入单个属性,用于弱注入
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws IllegalAccessException, InvocationTargetException {
        // 获取字段/ setter方法上的@Value注解和@Autowired注解
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        // 获取要注入的字段或者setter方法
        Field field = null;
        Method method = null;
        // 要注入的是字段
        if (acc instanceof Field f) {
            // 确保可注入
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method m) {
            checkFieldOrMethod(m);
            // 如果不符合setter方法
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        // 获取要注入的方法/字段的名字以及对应要注入的Bean类型
        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessiableType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        // @Value注入
        if (value != null) {
            Object propValue = this.propertyResolver.getRequiredProperty(value.value(), accessiableType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if (method != null) {
                logger.atDebug().log("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }
        }

        // @Autowired注入
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessiableType) : findBean(name, accessiableType);
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Mield injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    /**
     * Member是接口，是Field Method Constructor的接口
     */
    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field) {
                throw new BeanDefinitionException("Cannot inject final field: " + field);
            }
            if (m instanceof Method method) {
                logger.warn(
                        "Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }


    /**
     *
     * 创建一个Bean，但不进行字段和方法的注入。如果创建的Bean不是Configuration，则在构造方法 、工厂方法中注入
     */
    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        // 检测重复创建Bean导致的循环依赖
        // A->B->C->A，创建第二个A的时候就会导致循环依赖创建失败
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }

        // 创建方法：构造方法或工厂方法
        // 如果Bean定义中没有工厂方法就是自定义的Bean，直接构造方法创建，否则就是第三方bean，需要工厂方法创建
        Executable createFn = def.getFactoryName() == null ?
                def.getConstructor() : def.getFactoryMethod();

        // 创建参数
        // 获取创建方法（构造方法/工厂方法）的参数
        Parameter[] parameters = createFn.getParameters();
        // 获取每个参数的注解（一个参数可以有多个注解）
        Annotation[][] parametersAnnos = createFn.getParameterAnnotations();
        // 创建方法要设置的参数
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i += 1) {
            Parameter param = parameters[i];
            // 获取当前参数的注解
            Annotation[] paramAnnos = parametersAnnos[i];
            // 从参数获取 @Value 和 @Autowired
            Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            // @Configuration类型的bean是工厂，不能用autowired创建
            boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            //BeanPostProcessor 不能依赖其他Bean，不可以用@Autowired创建
            boolean isBeanPostProcessor = isBeanPostProcessorDefinition(def);
            if (isBeanPostProcessor && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create BeanPostProcessor '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数需要@Value或@Autowired两者之一，不能同时设置:
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数的类型
            Class<?> type = param.getType();
            if (value != null) {
                // 参数是 @Value 注解，从配置中获取配置值注入
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                // 参数是 @Autowired 注解
                String name = autowired.name();
                boolean required = autowired.value();
                // 寻找依赖的BeanDefinition
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                // 检测 required == true，即是否必须的依赖
                if (required && dependsOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }
                if (dependsOnDef != null) {
                    // 获取依赖Bean
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    if (autowiredBeanInstance == null && !isConfiguration) {
                        // 有一个递归调用
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    // reqired == false && 没有对应加载的BeanDefinition
                    args[i] = null;
                }
            }
        }

        // 如果存在依赖已经在上面解决完了，参数已经设置好，开始创建Bean实例
        Object instance = null;
        if (def.getFactoryName() == null) {
            // 用构造方法创建
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            // 用@Bean方法创建
            try {
                // 获取工厂Bean，即用@Configuration注解的类，需要工厂类调用@Bean工厂方法创建Bean
                Object configInstance = getBean(def.getFactoryName());
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        // 设置BeanDefinition的实例
        def.setInstance(instance);

        // 调用BeanPostProcessor处理Bean
        // 遍历实现beanPostProcessor的Bean列表
        for (BeanPostProcessor processor : beanPostProcessors) {
            // 为每一个创建的bean实例调用方法
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            // 如果一个BeanPostProcessor替换了原始Bean，则更新Bean的引用
            if (def.getInstance() != processed) {
                def.setInstance(processed);
            }
        }

        logger.atDebug().log("create bean instance: {}", def.getName());
        // 返回创建的Bean实例
        return def.getInstance();
    }

    /**
     * 获取代理的原始实例
     */
    private Object getProxiedInstance(BeanDefinition def) {
        Object beanInstance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入原始bean，则由BeanPostProcessor指定原始bean
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                beanInstance = restoredInstance;
            }
        }
        return beanInstance;
    }

    /**
     * 调用方法，用于init/destory方法
     */
    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        // 如果是@Component的Bean，直接调用方法；如果是@Bean工厂创建的Bean，需要通过方法名反射获得方法
        // 调用init/destory方法
        if (method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (namedMethod != null) {
            // 查找 initMethod/destoryMethod="xyz" 实在实际类型中寻找
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try {
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }
}
