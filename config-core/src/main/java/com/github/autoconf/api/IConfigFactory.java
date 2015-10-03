package com.github.autoconf.api;

/**
 * 获取配置的工厂类
 * Created by lirui on 2015/9/23.
 */
public interface IConfigFactory {
  /**
   * 获取一个IConfig对象
   *
   * @param name 配置名称，可以是逗号分隔的多个配置名
   * @return 配置对象
   */
  IChangeableConfig getConfig(String name);

  /**
   * 获取一个IConfig配置对象，并注册一个listener，注册完成会回调
   *
   * @param name     配置名
   * @param listener 配置更新回调listener
   * @return 配置对象
   */
  IChangeableConfig getConfig(String name, IChangeListener listener);

  /**
   * 获取一个IConfig配置对象，并注册一个listener，根据loadAfterRegister决定是否马上回调
   *
   * @param name              配置名
   * @param listener          配置更新回调listener
   * @param loadAfterRegister 注册listener之后是否马上回调
   * @return 配置对象
   */
  IChangeableConfig getConfig(String name, IChangeListener listener, boolean loadAfterRegister);

  /**
   * 是否加载了对应名称的配置
   *
   * @param name 配置名称
   * @return 如果已经加载对应配置则返回true
   */
  boolean hasConfig(String name);
}
