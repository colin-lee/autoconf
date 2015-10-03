package com.github.autoconf.base;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeable;
import com.github.autoconf.api.IChangeableConfig;

/**
 * 配置基类
 * Created by lirui on 2015/9/24.
 */
public class ChangeableConfig extends Config implements IChangeableConfig {
  private final String name;
  private final IChangeable eventBus;

  public ChangeableConfig(String name) {
    this.name = name;
    this.eventBus = new EventBus(this);
  }

  @Override
  public String getName() {
    return name;
  }

  public void addListener(IChangeListener listener) {
    eventBus.addListener(listener);
  }

  public void addListener(IChangeListener listener, boolean loadAfterRegister) {
    eventBus.addListener(listener, loadAfterRegister);
  }

  public void removeListener(IChangeListener listener) {
    eventBus.removeListener(listener);
  }

  public void notifyListeners() {
    eventBus.notifyListeners();
  }
}
