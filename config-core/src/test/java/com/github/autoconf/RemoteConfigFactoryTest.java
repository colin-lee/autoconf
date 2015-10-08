package com.github.autoconf;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.base.ProcessInfo;
import com.github.autoconf.helper.ConfigHelper;
import com.google.common.io.Closeables;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.autoconf.helper.ZookeeperUtil.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-10-01 23:01.
 */
public class RemoteConfigFactoryTest {
  private static TestingServer server;
  private static RemoteConfigFactory factory;

  @BeforeClass
  public static void beforeClass() throws Exception {
    server = new TestingServer();
    factory = new RemoteConfigFactory(ConfigHelper.getProcessInfo(), ConfigHelper.newClient(server.getConnectString()));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Closeables.close(factory.getClient(), true);
    Closeables.close(server, true);
  }

  @Test
  public void testFactory() throws Exception {
    ProcessInfo info = factory.getInfo();
    String name = "app.ini";
    String path = ZKPaths.makePath(info.getPath(), name, info.getProfile());
    final AtomicInteger num = new AtomicInteger(0);
    IChangeableConfig c = factory.getConfig(name, new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    });
    create(factory.getClient(), path, newBytes("a=1"));
    TestHelper.busyWait(num);
    assertThat(c.getInt("a"), is(1));

    num.set(0);
    setData(factory.getClient(), path, newBytes("a=2"));
    TestHelper.busyWait(num);
    assertThat(c.getInt("a"), is(2));
  }
}
