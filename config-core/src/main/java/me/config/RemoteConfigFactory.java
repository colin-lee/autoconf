package me.config;

import me.config.api.IChangeableConfig;
import me.config.base.AbstractConfigFactory;
import me.config.base.ProcessInfo;
import me.config.helper.Helper;
import me.config.impl.RemoteConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

/**
 * 只拉取zookeeper配置的工厂类
 * Created by lirui on 2015-10-01 22:25.
 */
public class RemoteConfigFactory extends AbstractConfigFactory {
  private final ProcessInfo info;
  private final CuratorFramework client;

  public RemoteConfigFactory(ProcessInfo info, CuratorFramework client) {
    this.info = info;
    this.client = client;
  }

  public static RemoteConfigFactory getInstance() {
    return LazyHolder.instance;
  }

  public ProcessInfo getInfo() {
    return info;
  }

  public CuratorFramework getClient() {
    return client;
  }

  /**
   * 创建LocalConfig并增加更新回调功能
   *
   * @param name 配置名
   * @return 配置
   */
  @Override
  protected IChangeableConfig doCreate(String name) {
    String path = ZKPaths.makePath(info.getPath(), name);
    RemoteConfig c = new RemoteConfig(name, path, info.orderedPath(), client);
    c.start();
    return c;
  }

  private static class LazyHolder {
    private static final RemoteConfigFactory instance =
      new RemoteConfigFactory(Helper.getProcessInfo(), Helper.createDefaultClient());
  }
}
