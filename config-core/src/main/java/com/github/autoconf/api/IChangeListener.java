package com.github.autoconf.api;

/**
 * 配置更新回调
 * Created by lirui on 2015/09/22.
 */
public interface IChangeListener {
  /**
   * 配置更新，回调注册的功能实现对应功能变更
   *
   * @param config 配置文件
   */
  void changed(IConfig config);
}
