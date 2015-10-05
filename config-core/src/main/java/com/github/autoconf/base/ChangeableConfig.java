package com.github.autoconf.base;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeable;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.helper.ZookeeperUtil;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

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


  /**
   * 判断新接收到的数据和以前相比是否发生了变化
   *
   * @param now 新数据
   * @return 逐字节对比，不一样就返回true
   */
  public boolean isChanged(byte[] now) {
    if (now == null) {
      return true;
    }
    byte[] old = getContent();
    LoggerFactory.getLogger(getClass()).debug("change detecting\nbefore:\n{}\n\nafter:\n{}\n", ZookeeperUtil.newString(old), ZookeeperUtil.newString(now));
    return !Arrays.equals(now, old);
  }
}
