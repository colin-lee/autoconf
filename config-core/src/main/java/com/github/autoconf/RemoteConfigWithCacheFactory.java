package com.github.autoconf;

import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.base.ProcessInfo;
import com.github.autoconf.helper.ConfigHelper;
import com.github.autoconf.impl.RemoteConfigWithCache;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import java.io.File;
import java.nio.file.Path;

/**
 * 只拉取zookeeper配置的工厂类
 * Created by lirui on 2015-10-01 22:25.
 */
public class RemoteConfigWithCacheFactory extends RemoteConfigFactory {
  private final Path path;

  public RemoteConfigWithCacheFactory(Path localConfigPath, ProcessInfo info, CuratorFramework client) {
    super(info, client);
    this.path = localConfigPath;
  }

  public static RemoteConfigWithCacheFactory getInstance() {
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
    ProcessInfo info = getInfo();
    String path = ZKPaths.makePath(info.getPath(), name);
    File cacheFile = this.path.resolve(name).toFile();
    RemoteConfigWithCache c = new RemoteConfigWithCache(name, path, info.orderedPath(), getClient(), cacheFile);
    c.start();
    return c;
  }

  private static class LazyHolder {
    private static final RemoteConfigWithCacheFactory instance = new RemoteConfigWithCacheFactory(ConfigHelper.getConfigPath(), ConfigHelper.getProcessInfo(), ConfigHelper.getDefaultClient());
  }
}
