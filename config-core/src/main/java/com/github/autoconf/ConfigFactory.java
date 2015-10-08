package com.github.autoconf;

import com.github.autoconf.api.IConfigFactory;
import com.github.autoconf.base.Config;
import com.github.autoconf.base.ProcessInfo;
import com.github.autoconf.helper.ConfigHelper;
import com.google.common.base.Strings;
import org.apache.curator.framework.CuratorFramework;

import java.nio.file.Path;

/**
 * 工厂类
 * Created by lirui on 2015-10-05 08:07.
 */
public class ConfigFactory {
  private ConfigFactory() {
  }

  public static IConfigFactory getInstance() {
    return LazyHolder.instance;
  }

  private static class LazyHolder {
    private static final IConfigFactory instance = newFactory();

    private static IConfigFactory newFactory() {
      Path localPath = ConfigHelper.getConfigPath();
      Config config = ConfigHelper.getApplicationConfig();
      // 找不到zookeeper的配置,则使用本地配置
      if (Strings.isNullOrEmpty(config.get("zookeeper.servers"))) {
        return new LocalConfigFactory(localPath);
      }

      ProcessInfo processInfo = ConfigHelper.getProcessInfo();
      CuratorFramework defaultClient = ConfigHelper.getDefaultClient();

      // 找不到配置的本地路径,则只用远程zookeeper配置
      if (System.getProperty("java.io.tmpdir").equals(localPath.toString())) {
        return new RemoteConfigFactory(processInfo, defaultClient);
      }

      // 使用远程zookeeper配置并启用本地cache功能
      return new RemoteConfigWithCacheFactory(localPath, processInfo, defaultClient);
    }
  }
}
