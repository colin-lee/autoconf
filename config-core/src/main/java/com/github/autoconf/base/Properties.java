package com.github.autoconf.base;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * 扩展map功能，可以获取不同类型的属性值
 *
 * Created by lirui on 15/9/28.
 */
public class Properties {
  private Map<String, String> m = ImmutableMap.of();

  public void copyOf(Map<String, String> items) {
    this.m = ImmutableMap.copyOf(items);
  }

  public int getInt(String key) {
    return getInt(key, 0);
  }

  public int getInt(String key, int defaultVal) {
    String val = get(key);
    if (!Strings.isNullOrEmpty(val)) {
      try {
        return Integer.parseInt(val);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultVal;
  }

  public long getLong(String key) {
    return getLong(key, 0L);
  }

  public long getLong(String key, long defaultVal) {
    String val = get(key);
    if (!Strings.isNullOrEmpty(val)) {
      try {
        return Long.parseLong(val);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultVal;
  }

  public boolean getBool(String key) {
    return getBool(key, false);
  }

  public boolean getBool(String key, boolean defaultVal) {
    String val = get(key);
    if (!Strings.isNullOrEmpty(val)) {
      return Boolean.parseBoolean(val);
    }
    return defaultVal;
  }

  public double getDouble(String key) {
    return getDouble(key, 0.0);
  }

  public double getDouble(String key, double defaultVal) {
    String val = get(key);
    if (!Strings.isNullOrEmpty(val)) {
      try {
        return Double.parseDouble(val);
      } catch (NumberFormatException ignored) {
      }
    }
    return defaultVal;
  }

  public String get(String key, String defaultVal) {
    String val = get(key);
    return val == null ? defaultVal : val;
  }

  public boolean has(String key) {
    return get(key) != null;
  }

  /**
   * 只有真正按照kv格式查找的时候，才进行解析对应kv内容。避免解析非KV格式的配置
   *
   * @param key 查找的key
   * @return 获取对应的value
   */
  public String get(String key) {
    return m.get(key);
  }

  public Map<String, String> getAll() {
    return m;
  }
}
