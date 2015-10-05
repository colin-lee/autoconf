package com.github.autoconf;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.base.ProcessInfo;
import com.github.autoconf.helper.ZookeeperUtil;
import com.github.autoconf.impl.RemoteConfigWithCache;
import com.google.common.io.Closeables;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-10-01 23:58.
 */
public class RemoteConfigWithCacheFactoryTest {
  private static TestingServer server;
  private static RemoteConfigWithCacheFactory factory;

  @BeforeClass
  public static void beforeClass() throws Exception {
    server = new TestingServer();
    //设置环境变量,覆盖application.properties配置
    System.setProperty("zookeeper.servers", server.getConnectString());
    factory = RemoteConfigWithCacheFactory.getInstance();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Closeables.close(factory.getClient(), true);
    Closeables.close(server, true);
  }

  @Test
  public void testFactory() throws Exception {
    String name = "factory.ini";
    File f = factory.getPath().resolve(name).toFile();
    TestHelper.writeFile(ZookeeperUtil.newBytes("a=-1"), f);
    ProcessInfo info = factory.getInfo();
    String path = ZKPaths.makePath(info.getPath(), name, info.getProfile());
    final AtomicInteger num = new AtomicInteger(0);
    RemoteConfigWithCache c = (RemoteConfigWithCache) factory.getConfig(name);
    //设定延迟1s启动zkClient
    c.setDelaySeconds(1);
    c.addListener(new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    }, false);
    assertThat(c.getInt("a"), is(-1));
    Thread.sleep(2000);
    ZookeeperUtil.create(factory.getClient(), path, ZookeeperUtil.newBytes("a=1"));
    TestHelper.busyWait(num);
    assertThat(c.getInt("a"), is(1));

    num.set(0);
    ZookeeperUtil.setData(factory.getClient(), path, ZookeeperUtil.newBytes("a=2"));
    TestHelper.busyWait(num);
    assertThat(c.getInt("a"), is(2));
    TestHelper.deleteFile(f);
  }
}
