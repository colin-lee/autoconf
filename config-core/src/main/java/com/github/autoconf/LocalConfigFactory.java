package com.github.autoconf;

import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IFileListener;
import com.github.autoconf.base.AbstractConfigFactory;
import com.github.autoconf.helper.ConfigHelper;
import com.github.autoconf.impl.LocalConfig;
import com.github.autoconf.watcher.FileUpdateWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 本地配置工厂
 * Created by lirui on 2015-09-30 22:25.
 */
public class LocalConfigFactory extends AbstractConfigFactory {
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfigFactory.class);

  private final Path path;

  public LocalConfigFactory(Path localConfigPath) {
    this.path = localConfigPath;
  }

  public static LocalConfigFactory getInstance() {
    return LazyHolder.instance;
  }

  /**
   * 本地cache根路径
   *
   * @return 路径
   */
  public Path getPath() {
    return path;
  }

  /**
   * 创建LocalConfig并增加更新回调功能
   *
   * @param name 配置名
   * @return 配置
   */
  @Override
  protected IChangeableConfig doCreate(String name) {
    Path p = path.resolve(name);
    final LocalConfig c = new LocalConfig(name, p);
    FileUpdateWatcher.getInstance().watch(p, new IFileListener() {
      @Override
      public void changed(Path path, byte[] content) {
        if (c.isChanged(content)) {
          LOG.info("{} changed", path);
          c.copyOf(content);
          c.notifyListeners();
        }
      }
    });
    return c;
  }

  private static class LazyHolder {
    private static final LocalConfigFactory instance = new LocalConfigFactory(ConfigHelper.getConfigPath());
  }
}
