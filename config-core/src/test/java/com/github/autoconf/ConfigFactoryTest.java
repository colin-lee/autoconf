package com.github.autoconf;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.base.ProcessInfo;
import com.github.autoconf.helper.ZookeeperUtil;
import com.github.autoconf.impl.RemoteConfigWithCache;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.ZKPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-10-01 23:58.
 */
public class ConfigFactoryTest {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigFactoryTest.class);
  private static TestingServer server;

  @BeforeClass
  public static void beforeClass() throws Exception {
    server = new TestingServer();
    //设置环境变量,覆盖application.properties配置
    System.setProperty("zookeeper.servers", server.getConnectString());
  }

  @AfterClass
  public static void afterClass() throws Exception {
    Closeables.close(server, true);
  }

  @Test
  public void testFactory() throws Exception {
    ConfigFactory factory = ConfigFactory.getInstance();
    String name = "app.txt";
    File f = factory.getPath().resolve(name).toFile();
    write(ZookeeperUtil.newBytes("b=1"), f);
    ProcessInfo info = factory.getInfo();
    String path = ZKPaths.makePath(info.getPath(), name, info.getProfile());
    final AtomicInteger num = new AtomicInteger(0);
    RemoteConfigWithCache c = (RemoteConfigWithCache) factory.getConfig(name);
    c.setDelaySeconds(1);
    c.addListener(new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    }, false);
    assertThat(c.getInt("b"), is(1));
    Thread.sleep(2000);
    ZookeeperUtil.create(factory.getClient(), path, ZookeeperUtil.newBytes("a=1"));
    busyWait(num);
    assertThat(c.getInt("a"), is(1));

    num.set(0);
    ZookeeperUtil.setData(factory.getClient(), path, ZookeeperUtil.newBytes("a=2"));
    busyWait(num);
    assertThat(c.getInt("a"), is(2));
    delete(f);
  }

  private void busyWait(final AtomicInteger num) throws InterruptedException {
    int tries = 0;
    while (++tries < 600) {
      Thread.sleep(100);
      if (num.get() > 0) {
        LOG.info("delay {} ms", 100 * tries);
        return;
      }
    }
    LOG.error("detect timeout, delay {}ms", 100 * tries);
  }

  private void write(byte[] bytes, File f) throws IOException {
    LOG.info("write {} bytes into {}", bytes.length, f);
    Files.write(bytes, f);
  }

  private void delete(File f) {
    if (!f.exists())
      return;
    LOG.info("delete {}", f);
    if (!f.delete()) {
      f.deleteOnExit();
    }
  }
}
