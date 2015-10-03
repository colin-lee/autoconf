package com.github.autoconf.base;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IConfigFactory;
import com.github.autoconf.impl.MergedConfig;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * 配置工厂抽象类
 * Created by lirui on 2015-09-30 22:25.
 */
public abstract class AbstractConfigFactory implements IConfigFactory {
  private final ConcurrentMap<String, IChangeableConfig> m = Maps.newConcurrentMap();

  @Override
  public IChangeableConfig getConfig(String name) {
    IChangeableConfig c = m.get(name);
    if (c == null) {
      synchronized (this) {
        c = m.get(name);
        if (c == null) {
          c = newConfig(name);
          IChangeableConfig real = m.putIfAbsent(name, c);
          if (real != null) {
            c = real;
          }
        }
      }
    }
    return c;
  }

  @Override
  public IChangeableConfig getConfig(String name, IChangeListener listener) {
    return getConfig(name, listener, true);
  }

  @Override
  public IChangeableConfig getConfig(String name, IChangeListener listener, boolean loadAfterRegister) {
    IChangeableConfig config = getConfig(name);
    config.addListener(listener, loadAfterRegister);
    return config;
  }

  @Override
  public boolean hasConfig(String name) {
    return m.containsKey(name);
  }

  private IChangeableConfig newConfig(String name) {
    CharMatcher matcher = CharMatcher.anyOf(",; |");
    if (matcher.matchesAnyOf(name)) {
      List<String> names = Splitter.on(matcher).trimResults().omitEmptyStrings().splitToList(name);
      List<IChangeableConfig> list = Lists.newArrayList();
      for (String i : names) {
        list.add(getConfig(i));
      }
      return new MergedConfig(list);
    } else {
      return doCreate(name);
    }
  }

  protected abstract IChangeableConfig doCreate(String name);
}
