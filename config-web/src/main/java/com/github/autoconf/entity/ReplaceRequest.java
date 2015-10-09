package com.github.autoconf.entity;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;

/**
 * 替换配置内容的请求
 *
 * Created by lirui on 2015/03/02 上午10:44.
 */
public class ReplaceRequest implements Serializable {
  @NotNull
  private String src;
  @NotNull
  private String dst;
  private Set<Long> configIds;
  private Config config;
  private String oldLines;
  private String newLines;

  public String getSrc() {
    return src;
  }

  public void setSrc(String src) {
    this.src = src;
  }

  public String getDst() {
    return dst;
  }

  public void setDst(String dst) {
    this.dst = dst;
  }

  public Config getConfig() {
    return config;
  }

  public void setConfig(Config config) {
    this.config = config;
  }

  public Set<Long> getConfigIds() {
    return configIds;
  }

  public void setConfigIds(Set<Long> configIds) {
    this.configIds = configIds;
  }

  public String getOldLines() {
    return oldLines;
  }

  public void setOldLines(String oldLines) {
    this.oldLines = oldLines;
  }

  public String getNewLines() {
    return newLines;
  }

  public void setNewLines(String newLines) {
    this.newLines = newLines;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ReplaceRequest{");
    sb.append("src='").append(src).append('\'');
    sb.append(", dst='").append(dst).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
