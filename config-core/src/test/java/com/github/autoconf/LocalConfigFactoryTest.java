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
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.autoconf.helper.ZookeeperUtil.newBytes;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

/**
 * 测试工厂类
 * Created by lirui on 2015-09-30 22:49.
 */
public class LocalConfigFactoryTest {
  private static final Logger LOG = LoggerFactory.getLogger(LocalConfigFactoryTest.class);

  @Test
  public void testFactory() throws Exception {
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      return;
    }
    File dir = Files.createTempDir();
    File f1 = dir.toPath().resolve("f1").toFile();
    File f2 = dir.toPath().resolve("f2").toFile();
    File f3 = dir.toPath().resolve("f3").toFile();
    try {
      FileUpdateWatcher.getInstance().start();
      TestHelper.writeFile(newBytes("a=1"), f1);
      TestHelper.writeFile(newBytes("a=2\nb=2"), f2);
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
      byte[] bytes = newBytes("a=3");
      TestHelper.writeFile(bytes, f1);
      assertArrayEquals(bytes, Files.toByteArray(f1));
      TestHelper.busyWait(num);
      assertThat(merge.getInt("a"), is(3));

      num.set(0);
      TestHelper.writeFile(newBytes("c=4"), f3);
      TestHelper.busyWait(num);
      assertThat(merge.getInt("c"), is(4));
    } finally {
      TestHelper.deleteFile(f1);
      TestHelper.deleteFile(f2);
      TestHelper.deleteFile(f3);
      TestHelper.deleteFile(dir);
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
}
