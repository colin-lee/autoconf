package me.config;

import me.config.api.IChangeableConfig;
import me.config.api.IConfigFactory;
import me.config.base.AbstractConfigFactory;
import me.config.impl.ZookeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;

/**
 * 本地配置工厂
 * Created by lirui on 2015-09-30 22:25.
 */
public class ZookeeperConfigFactory extends AbstractConfigFactory {
  private final String zkPath;
  private final CuratorFramework client;

  public ZookeeperConfigFactory(String zkPath, CuratorFramework client) {
    this.zkPath = zkPath;
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
    return new ZookeeperConfig(name, zkPath, null, client);
  }

  private static class LazyHolder {
    private static final IConfigFactory instance = create();

    private static IConfigFactory create() {
      CuratorFramework client = CuratorFrameworkFactory.builder().build();
      return null;
    }
  }
}
