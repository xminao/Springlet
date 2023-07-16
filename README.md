![springlet-logo](https://github.com/xminao/Springlet/raw/master/imgs/springlet-logo.png)



### 项目介绍 📌

---

​	**Springlet Framework** 是一个模仿 [Spring Framework](https://github.com/spring-projects/spring-framework) 打造的小型J2EE应用程序开发框架，命名灵感来自于在CS61B中实现的小型版本控制 [Gitlet](https://github.com/xminao/Gitlet)，目前已经实现了最核心的IoC容器、AOP，以及JdbcTemplate！



### IoC容器 📦

---

​	框架最核心的功能，一个通过**依赖注入**(Dependency Injection)实现**控制反转**(Inversion Of Control)的容器，用于管理Bean的生命周期以及配置Bean的依赖关系。

​	下面列举了本框架容器的功能：

| 容器功能 | 实现方式                             |
| -------- | ------------------------------------ |
| 容器接口 | ApplicationContext                   |
| 配置Bean | 注解                                 |
| 扫描Bean | 按包名扫描                           |
| Bean类型 | 单例模式（Singleton）                |
| Bean定制 | BeanPostProcessor                    |
| 依赖注入 | 构造方法、工厂方法、Setter方法、字段 |
| Bean工厂 | @Bean注解                            |



### AOP 🎭

---

​	通过AOP（Aspect-oriented Programming，面向切面编程）可以实现无侵入增强功能，本质是一种代理模式，让容器在运行时可以自行组织如何增强，核心是对要增强的Bean进行方法拦截。

- **代理实现**：采用运行时织入字节码方式，具体实现使用ByteBuddy库。

- **代理定义**：不支持AspectJ的语法，仅支持通过注解定义，例如@Transactional开启事务。

​	本框架内置了一个@Around注解和AroundProxyBeanPostProcessor来实现AOP，用户使用AOP功能需要自己提供一个带@Around注解的Bean和一个实现JDK拦截器InvocationHandler的Bean。

> 💡 因为CGLib已经停止维护，所以使用ByteBuddy



### JdbcTemplate 📑

---

​	通过IoC容器以及AOP就可以实现JDBC和声明式事务，本框架提供JdbcTemplate，一个使用模板方法实现的操作类，封装了基本的Jdbc操作。事务传播类型仅支持`REQUIRED`，也即有事务就加入，没事务就自己创建。

​	JdbcTemplate使用模板方法以及大量回调实现，连接池默认使用HikariCP。



### 未完待续... 🔗

---

后续还要快速启动Boot功能等。