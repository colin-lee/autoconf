package com.github.autoconf.base;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 根据缓存获取内容，支持纯文本或者KV格式的解析。并且使用懒加载模式，只在需要的时候做解析。
 * Created by lirui on 2015/9/23.
 */
public class Config extends Properties {
  public static final Charset UTF8 = Charset.forName("UTF-8");
  public static final Charset GBK = Charset.forName("GBK");
  private boolean parsed = false;
  private byte[] content;

  public synchronized byte[] getContent() {
    if (content == null) {
      Map<String, String> m = getAll();
      if (m.isEmpty()) {
        content = new byte[0];
      } else {
        StringBuilder sbd = new StringBuilder();
        for (Map.Entry<String, String> i : m.entrySet()) {
          sbd.append(i.getKey()).append('=').append(i.getValue()).append('\n');
        }
        content = sbd.toString().getBytes(UTF8);
      }
    }
    return content;
  }

  public void copyOf(String s) {
    this.content = s.getBytes(UTF8);
    parsed = false;
  }

  public void copyOf(byte[] content) {
    this.content = content;
    parsed = false;
  }

  @Override
  public void copyOf(Map<String, String> m) {
    super.copyOf(m);
    parsed = true;
    this.content = null;
  }

  private synchronized void parse() {
    if (!parsed) {
      Map<String, String> m = Maps.newLinkedHashMap();
      final byte[] bytes = content;
      if (bytes != null) {
        String txt = new String(bytes, UTF8);
        for (String i : lines(txt, true)) {
          int pos = i.indexOf('=');
          if (pos != -1 && (pos + 1) < i.length()) {
            String k = i.substring(0, pos).trim();
            String v = i.substring(pos + 1).trim();
            m.put(k, v);
          }
        }
        super.copyOf(m);
      }
      parsed = true;
    }
  }

  @Override
  public String get(String key) {
    if (!parsed) {
      parse();
    }
    return super.get(key);
  }

  @Override
  public Map<String, String> getAll() {
    if (!parsed) {
      parse();
    }
    return super.getAll();
  }

  public String getString() {
    return new String(getContent(), UTF8);
  }

  public String getString(Charset charset) {
    return new String(getContent(), charset);
  }

  public List<String> getLines() {
    return getLines(UTF8, true);
  }

  public List<String> getLines(Charset charset) {
    return lines(new String(getContent(), charset), true);
  }

  public List<String> getLines(Charset charset, boolean removeComment) {
    return lines(new String(getContent(), charset), removeComment);
  }

  private List<String> lines(String s, boolean removeComment) {
    List<String> raw = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(s);
    if (!removeComment)
      return raw;

    List<String> clean = Lists.newArrayList();
    for (String i : raw) {
      if (i.charAt(0) == '#' || i.startsWith("//"))
        continue;
      clean.add(i);
    }
    return clean;
  }
}
