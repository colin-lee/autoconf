spring-support
===

来源
===
1. ReloadableProperty 从 https://github.com/jamesmorgan/ReloadablePropertiesAnnotation.git 复制修改而来

目的
===
1. ReloadablePropertySourcesPlaceholderConfigurer支持动态加载cms和本地文件系统的配置


功能
===
1. ReloadableProperty 提供可重加载的配置

示例
===
maven配置
===
    <dependency>
        <groupId>com.github.colin-lee</groupId>
        <artifactId>spring-support</artifactId>
        <version>1.0.0</version>
    </dependency>

使用ReloadablePropertySourcesPlaceholderConfigurer的spring配置文件
====
    <bean class="com.github.autoconf.spring.reloadable.ReloadablePropertyPostProcessor">
            <constructor-arg ref="propertyConfigurator"/>
    </bean>

    <bean id="propertyConfigurator" class="com.github.autoconf.spring.reloadable.ReloadablePropertySourcesPlaceholderConfigurer">
        <property name="ignoreUnresolvablePlaceholders" value="false"/>
        <property name="ignoreResourceNotFound" value="true"/>
        <!-- 可以是逗号分隔的多个配置文件名 -->
        <property name="configName" value="app.properties"/>
        <!-- 支持本地文件系统配置 -->
        <property name="locations">
            <list>
                <value>classpath:j4log.properties</value>
            </list>
        </property>
    </bean>

    <!-- 通过 ${} 引用的变量有更改的时候，会自动更新对应属性 -->
    <bean class="com.github.autoconf.support.spring.reloadable.Article">
        <property name="author" value="${article.author}"/>
        <property name="content" value="${article.content}, welcome"/>
    </bean>

@Value注解
=====
可以通过@Value("${key:defaultValue}")来获取对应值，但是对应的值不会做自动更新，例如：

    @Value("${article.title:default title}")
    private String title;
@ReloadableProperty注解
=====
可以通过@ReloadableProperty("key")来注解，对应的值会自动更新，默认值通过变量定义指定。例如：

    @ReloadableProperty("dynamic")
    private String dynamic = "default";
