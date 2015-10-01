package me.config;

import me.config.api.IChangeableConfig;
import me.config.api.IFileListener;
import me.config.base.AbstractConfigFactory;
import me.config.helper.Helper;
import me.config.impl.LocalConfig;
import me.config.watcher.FileUpdateWatcher;

import java.nio.file.Path;

/**
 * 本地配置工厂
 * Created by lirui on 2015-09-30 22:25.
 */
public class LocalConfigFactory extends AbstractConfigFactory {
  private final Path basePath;

  public LocalConfigFactory(Path localConfigPath) {
    this.basePath = localConfigPath;
  }

  public static LocalConfigFactory getInstance() {
    return LazyHolder.instance;
  }

  /**
   * 创建LocalConfig并增加更新回调功能
   *
   * @param name 配置名
   * @return 配置
   */
  @Override
  protected IChangeableConfig doCreate(String name) {
    Path path = basePath.resolve(name);
    final LocalConfig c = new LocalConfig(name, path);
    FileUpdateWatcher.getInstance().watch(path, new IFileListener() {
      @Override
      public void changed(Path path, byte[] content) {
        log.info("{} changed", path);
        c.copyOf(content);
        c.notifyListeners();
      }
    });
    return c;
  }

  private static class LazyHolder {
    private static final LocalConfigFactory instance =
      new LocalConfigFactory(Helper.getConfigPath());
  }
}
