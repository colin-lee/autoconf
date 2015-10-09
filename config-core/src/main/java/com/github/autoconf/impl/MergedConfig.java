package com.github.autoconf.impl;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.base.ChangeableConfig;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 合并多个配置文件的内容为一个，同名配置，排在前面的优先。
 * <pre>
 * 1. 仅支持kv类型的合并
 * 2. 仅支持UTF8编码
 * </pre>
 * Created by lirui on 15/9/24.
 */
public class MergedConfig extends ChangeableConfig implements IChangeableConfig {
  private final List<IChangeableConfig> configs;

  public MergedConfig(List<IChangeableConfig> configs) {
    super(Joiner.on(',').join(Collections2.transform(configs, new Function<IChangeableConfig, String>() {
      @Override
      public String apply(IChangeableConfig input) {
        return input.getName();
      }
    })));

    IChangeListener listener = new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        merge();
      }
    };

    // 注册单个配置文件的更新回调功能
    for (IChangeableConfig c : configs) {
      c.addListener(listener, false);
    }

    // 同名配置，排在前面的优先，所以按照做一次排序反转
    this.configs = Lists.newArrayList(configs);
    Collections.reverse(this.configs);

    // 首次merge配置
    merge();
  }

  private void merge() {
    Map<String, String> m = Maps.newHashMap();
    for (IChangeableConfig c : this.configs) {
      m.putAll(c.getAll());
    }
    copyOf(m);
    notifyListeners();
  }

  @Override
  public String toString() {
    return "MergedConfig{" + "name=" + getName() + '}';
  }
}
