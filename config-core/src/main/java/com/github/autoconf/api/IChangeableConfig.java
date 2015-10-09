package com.github.autoconf.api;

/**
 * 配置文件
 * Created by lirui on 2015/09/22.
 */
public interface IChangeableConfig extends IConfig, IChangeable {
  /**
   * 配置文件名
   *
   * @return 配置文件名
   */
  String getName();
}
