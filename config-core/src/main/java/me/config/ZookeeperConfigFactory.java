package me.config;

import me.config.api.IChangeableConfig;
import me.config.base.AbstractConfigFactory;
import me.config.base.ProcessInfo;
import me.config.helper.Helper;
import me.config.impl.ZookeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

/**
 * 只拉取zookeeper配置的工厂类
 * Created by lirui on 2015-10-01 22:25.
 */
public class ZookeeperConfigFactory extends AbstractConfigFactory {
  private final ProcessInfo info;
  private final CuratorFramework client;

  public ZookeeperConfigFactory(ProcessInfo info, CuratorFramework client) {
    this.info = info;
    this.client = client;
  }

  public static ZookeeperConfigFactory getInstance() {
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
    ZookeeperConfig c = new ZookeeperConfig(name, path, info.orderedPath(), client);
    c.start();
    return c;
  }

  private static class LazyHolder {
    private static final ZookeeperConfigFactory instance =
      new ZookeeperConfigFactory(Helper.getProcessInfo(), Helper.createDefaultClient());
  }
}
