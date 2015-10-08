package com.github.autoconf.base;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * 获取进程运行时信息
 * Created by lirui on 2015-09-28 20:34.
 */
public class ProcessInfo {
  private String path;
  private String name;
  private String profile;
  private String ip;
  private String port;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public List<String> orderedPath() {
    List<String> paths = Lists.newArrayList();
    if (!Strings.isNullOrEmpty(ip)) {
      if (!Strings.isNullOrEmpty(port)) {
        paths.add(ip + ':' + port);
      }
      paths.add(ip);
    }
    if (!Strings.isNullOrEmpty(profile)) {
      paths.add(profile);
    }
    if (!Strings.isNullOrEmpty(name)) {
      paths.add(name);
    }
    return paths;
  }

  @Override
  public String toString() {
    return "ProcessInfo{" + "path=" + path + ", name=" + name + ", profile=" + profile + ", ip=" + ip + ", port=" + port + '}';
  }
}
