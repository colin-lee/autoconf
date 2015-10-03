package com.github.autoconf.api;

/**
 * 配置更改回调功能
 * Created by lirui on 15/9/24.
 */
public interface IChangeable {
  /**
   * 注册更新回调方法，并且会马上调用1次回调函数，避免外层还需要手动调用1次
   *
   * @param listener 更新回调方法
   */
  void addListener(IChangeListener listener);

  /**
   * 注册更新回调方法
   *
   * @param listener          更新回调方法
   * @param loadAfterRegister 注册后立即调用回调函数
   */
  void addListener(IChangeListener listener, boolean loadAfterRegister);

  /**
   * 去掉listener
   *
   * @param listener 更新回调函数
   */
  void removeListener(IChangeListener listener);

  /**
   * 通知所有注册的回调函数
   */
  void notifyListeners();
}
