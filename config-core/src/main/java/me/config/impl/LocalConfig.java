package me.config.impl;

import com.google.common.io.Files;
import me.config.base.ChangeableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 基于本地文件的配置
 * Created by lirui on 2015/9/23.
 */
public class LocalConfig extends ChangeableConfig {
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfig.class);
  private final Path path;

  public LocalConfig(String name, Path path) {
    super(name);
    this.path = path;
    try {
      if (path.toFile().exists()) {
        copyOf(Files.toByteArray(path.toFile()));
      }
    } catch (IOException e) {
      copyOf(new byte[0]);
      LOG.error("configName={}, path={}", name, path, e);
    }
  }

  public Path getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "LocalConfig{" + "name=" + getName() + ", path=" + path + '}';
  }
}
