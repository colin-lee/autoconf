package com.github.autoconf.entity;

/**
 * 配置对象
 *
 * Created by lirui on 15/1/10.
 */
public class ConfigHistory extends Config {
  private long configId;

  public long getConfigId() {
    return configId;
  }

  public void setConfigId(long configId) {
    this.configId = configId;
  }

  public void copy(Config old) {
    this.setVersion(old.getVersion());
    this.setName(old.getName());
    this.setProfile(old.getProfile());
    this.setPath(old.getPath());
    this.setContent(old.getContent());
    this.setEditor(old.getEditor());
    this.setModifyTime(old.getModifyTime());
    this.setConfigId(old.getId());
  }

  @Override
  public String toString() {
    return "ConfigHistory{" + "name=" + getName() + ',' + "profile=" + getProfile() + ',' + "version=" + getVersion() + '}';
  }
}
