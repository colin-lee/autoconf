##配置管理系统(ConfigManagerSystem)

[![Build Status](https://travis-ci.org/colin-lee/autoconf.svg?branch=master)](https://travis-ci.org/colin-lee/autoconf)
[![Coverage Status](https://coveralls.io/repos/colin-lee/autoconf/badge.png?branch=master&service=github)](https://coveralls.io/github/colin-lee/autoconf?branch=master)

###设计理念
- 基于Zookeeper集群实现集中式、实时更新配置
- 基于本地FileCache实现冷启动加速、去中心强依赖功能
- 扫描本地FileCache的修改，调试时临时修改参数不必通过web界面操作，避免生成很多特例配置

> 每个配置文件对应zookeeper的一个path，并且会把这个内容写入到一个本地目录下的同名文件中。
> 会有一个异步线程扫描本地文件的修改，若有修改会触发重新加载。
> 本地修改的内容会在服务重启以及zookeeper重写覆盖，只能是临时调试使用。

###本地配置目录
- 可以通过环境变量 -DlocalConfigPath=/data/config 来指明本地配置目录
- 如果不指定会从当前类路径下查找 autoconf 名字的目录，找到就会使用
- 找不到 autoconf 目录，则会尝试创建一个 autoconf
- 创建失败，则会使用 java.io.tmpdir 环境变量指明的目录作为本地配置目录

###远程Zookeeper配置目录
- 可以通过环境变量 -DzkConfigPath=/cms/config 来指明远程配置根目录
- 可以通过环境变量 -Dprocess.profile=dev -Dprocess.name=appName 来指明进程信息，用于定位具体配置

###配置大小限制
受zookeeper的默认配置限制，目前设定为最大 **1M** 。如果超过1M，抱歉，目前不支持。
这类大的配置文件，应该走发布系统进行下发，直接保存到对应机器中，不必一直放到zookeeper的内存镜像中。
而且这类大文件也不适合通过web界面进行编辑。

###配置文件编码
默认都是UTF8编码


###配置文件命名
- 匹配字符集\[a-zA-Z_-\]
- 文件名不要超过128个字符

###KV格式配置
- 提供各种数据类型的get方法，比如
```
getInt(String key, int defaultVal)
getInt(String key)
如果不提供默认值，相当于数字类型默认为0，bool类型默认为false。
```        
- 判断是否有对应配置，`has(String key)`

###文本格式配置
提供获取纯文本内容，所有文本行的功能。默认会删掉“\#”和“//”开头的注释文本行

- `getString()` 返回UTF8解码的文本内容
- `getString(Charset charset)` 返回指定编码的文本内容
- `getLines()` 返回UTF8解码的所有文本行
- `getLines(Charset charset)` 返回指定编码的所有文本行
- `getLines(Charset charset, boolean removeComment)` 根据是否去除注释指定，返回对应编码所有文本行

###二进制格式配置
- `getContent()` 返回配置的原始字节流，可以自己进行解析对应的byte\[\]内容

###配置加载优先顺序
默认在zookeeper上创建 /cms/config 根目录。如果当前进程信息如下

    process.team = teamName
    process.profile = deploy
    process.name = view-main
    process.ip = 10.204.8.32
    process.port = 8011


以demo.ini的配置为例，根据当前进程process.properties的配置，会按照下面顺序加载

1. `/cms/config/teamName/app/demo.ini/10.204.8.32:8011` 针对正式环境10.204.8.32:8011这个进程实例的特殊配置
2. `/cms/config/teamName/app/demo.ini/10.204.8.32` 针对正式环境10.204.8.32这个机器的特殊配置
3. `/cms/config/teamName/app/demo.ini/deploy` 针对正式环境的配置
4. `/cms/config/teamName/app/demo.ini/view-main` 针对view-main业务的特殊配置


###配置更新回调
####简单kv格式
不需要自己添加回调功能，系统会自动更新每个IConfig对象的数据，这样`config.getInt()`这样的函数每次都能拿到最新配置
####自定义回调函数
- 回调接口定义

```java
public interface IChangeListener {
    /**
     * 配置更新，回调注册的功能实现对应功能变更
     *
     * @param config 配置文件
     */
    void changed(IConfig config);
}
```
- 回调注册

```java
/**
 * 注册更新回调方法，并且会马上调用1次回调函数，避免外层还需要手动调用1次
 *
 * @param listener 更新回调方法
 */
void addListener(IChangeListener listener);
```
```java
/**
 * 注册更新回调方法
 *
 * @param listener 更新回调方法
 * @param loadAfterRegister 注册后立即调用回调函数
 */
void addListener(IChangeListener listener, boolean loadAfterRegister);
```

- 回调注销

```java
/**
 * 去掉listener
 *
 * @param listener 更新回调函数
 */
public void removeListener(IChangeListener listener);
```

###使用样例
获取一个config对象，并注册自己的更新回调函数，默认不需要

```java
public class ConfigDemo {
    static {
        ConfigFactory.getConfig("server.ini", new IChangeListener() {
            @Override
            public void changed(IConfig config) {
                loadConfig(config);
            }
        });
    }

    private static void loadConfig(IConfig config) {
        //do something
        int aInt = config.getInt("key");
    }
 }
```

