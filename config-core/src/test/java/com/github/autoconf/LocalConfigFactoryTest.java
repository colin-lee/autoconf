package com.github.autoconf;

import com.github.autoconf.api.IChangeListener;
import com.github.autoconf.api.IChangeableConfig;
import com.github.autoconf.api.IConfig;
import com.github.autoconf.api.IConfigFactory;
import com.github.autoconf.watcher.FileUpdateWatcher;
import com.google.common.io.Files;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.autoconf.helper.ZookeeperUtil.newBytes;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-09-30 22:49.
 */
public class LocalConfigFactoryTest {
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfigFactoryTest.class);

  @Test
  public void testFactory() throws Exception {
    File dir = Files.createTempDir();
    File f1 = dir.toPath().resolve("f1").toFile();
    File f2 = dir.toPath().resolve("f2").toFile();
    File f3 = dir.toPath().resolve("f3").toFile();
    try {
      FileUpdateWatcher.getInstance().start();
      write(newBytes("a=1"), f1);
      write(newBytes("a=2\nb=2"), f2);
      LocalConfigFactory factory = new LocalConfigFactory(dir.toPath());
      IChangeableConfig c1 = factory.getConfig("f1");
      IChangeableConfig merge = factory.getConfig("f1,f2,f3");
      assertThat(c1.getInt("a"), is(1));
      assertThat(merge.getInt("a"), is(1));
      assertThat(merge.getInt("b"), is(2));
      //测试本地配置修改更新
      final AtomicInteger num = new AtomicInteger(0);
      merge.addListener(new IChangeListener() {
        @Override
        public void changed(IConfig config) {
          LOG.info("{} changed", config.getName());
          num.incrementAndGet();
        }
      }, false);
      write(newBytes("a=3"), f1);
      busyWait(num);
      assertThat(merge.getInt("a"), is(3));

      num.set(0);
      write(newBytes("c=4"), f3);
      busyWait(num);
      assertThat(merge.getInt("c"), is(4));
    } finally {
      delete(f1);
      delete(f2);
      delete(f3);
      delete(dir);
    }
  }

  @Test
  public void testInstance() throws Exception {
    IConfigFactory factory = LocalConfigFactory.getInstance();
    final AtomicInteger num = new AtomicInteger(0);
    IChangeableConfig c = factory.getConfig("exist.ini", new IChangeListener() {
      @Override
      public void changed(IConfig config) {
        num.incrementAndGet();
      }
    });
    assertThat(c.getInt("a"), is(1));
    assertThat(num.get(), is(1));
  }

  private void busyWait(final AtomicInteger num) throws InterruptedException {
    int tries = 0;
    while (++tries < 60) {
      Thread.sleep(1000);
      if (num.get() > 0) {
        LOG.info("delay {} ms", 1000 * tries);
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
