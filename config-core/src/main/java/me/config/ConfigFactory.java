package me.config;

import me.config.api.IChangeableConfig;
import me.config.base.ProcessInfo;
import me.config.helper.Helper;
import me.config.impl.RemoteConfigWithCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import java.io.File;
import java.nio.file.Path;

/**
 * 只拉取zookeeper配置的工厂类
 * Created by lirui on 2015-10-01 22:25.
 */
public class ConfigFactory extends RemoteConfigFactory {
  private final Path basePath;

  public ConfigFactory(Path localConfigPath, ProcessInfo info, CuratorFramework client) {
    super(info, client);
    this.basePath = localConfigPath;
  }

  public static ConfigFactory getInstance() {
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
    ProcessInfo info = getInfo();
    String path = ZKPaths.makePath(info.getPath(), name);
    File cacheFile = basePath.resolve(name).toFile();
    RemoteConfigWithCache c =
      new RemoteConfigWithCache(name, path, info.orderedPath(), getClient(), cacheFile);
    c.start();
    return c;
  }

  private static class LazyHolder {
    private static final ConfigFactory instance =
      new ConfigFactory(Helper.getConfigPath(), Helper.getProcessInfo(), Helper.createDefaultClient());
  }
}
