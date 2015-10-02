package me.config;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import me.config.api.IChangeListener;
import me.config.api.IConfig;
import me.config.base.ProcessInfo;
import me.config.impl.RemoteConfigWithCache;
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

import static me.config.zookeeper.ZookeeperUtil.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-10-01 23:58.
 */
public class ConfigFactoryTest {
  private static TestingServer server;
  private Logger log = LoggerFactory.getLogger(getClass());

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
    write(newBytes("b=1"), f);
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
    create(factory.getClient(), path, newBytes("a=1"));
    busyWait(num);
    assertThat(c.getInt("a"), is(1));

    num.set(0);
    setData(factory.getClient(), path, newBytes("a=2"));
    busyWait(num);
    assertThat(c.getInt("a"), is(2));
    delete(f);
  }

  private void busyWait(final AtomicInteger num) throws InterruptedException {
    int tries = 0;
    while (++tries < 600) {
      Thread.sleep(100);
      if (num.get() > 0) {
        log.info("delay {} ms", 100 * tries);
        return;
      }
    }
    log.error("detect timeout, delay {}ms", 100 * tries);
  }

  private void write(byte[] bytes, File f) throws IOException {
    log.info("write {} bytes into {}", bytes.length, f);
    Files.write(bytes, f);
  }

  private void delete(File f) {
    if (!f.exists())
      return;
    log.info("delete {}", f);
    if (!f.delete()) {
      f.deleteOnExit();
    }
  }
}
