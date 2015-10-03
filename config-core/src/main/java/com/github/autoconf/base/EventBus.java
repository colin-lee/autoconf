package com.github.autoconf.base;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeable;
import com.github.autoconf.api.IConfig;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 消息总线，允许注册消息
 * Created by lirui on 15/9/24.
 */
public class EventBus implements IChangeable {
  private static final Logger LOG = LoggerFactory.getLogger(EventBus.class);
  private final Set<IChangeListener> listeners = Sets.newConcurrentHashSet();
  private final IConfig config;

  public EventBus(IConfig config) {
    this.config = config;
  }

  public void addListener(IChangeListener listener) {
    addListener(listener, true);
  }

  public void addListener(IChangeListener listener, boolean loadAfterRegister) {
    if (listener != null && !listeners.contains(listener)) {
      listeners.add(listener);
      if (loadAfterRegister) {
        try {
          listener.changed(config);
        } catch (Exception e) {
          LOG.error("cannot reload " + config.getName(), e);
        }
      }
    }
  }

  public void removeListener(IChangeListener listener) {
    if (listener != null) {
      listeners.remove(listener);
    }
  }

  public void notifyListeners() {
    for (IChangeListener i : listeners) {
      LOG.info("{} changed, notify {}", config.getName(), i);
      try {
        i.changed(config);
      } catch (Exception e) {
        LOG.error("cannot reload " + config.getName(), e);
      }
    }
  }
}
