package me.config;

import me.config.api.IChangeableConfig;
import me.config.api.IConfigFactory;
import me.config.base.AbstractConfigFactory;
import me.config.base.ProcessInfo;
import me.config.helper.Helper;
import me.config.impl.ZookeeperConfig;
import org.apache.curator.framework.CuratorFramework;

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

  public static IConfigFactory getInstance() {
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
    return new ZookeeperConfig(name, info.getPath(), info.orderedPath(), client);
  }

  private static class LazyHolder {
    private static final IConfigFactory instance =
      new ZookeeperConfigFactory(Helper.scanProcessInfo(), Helper.createDefaultClient());
  }
}
