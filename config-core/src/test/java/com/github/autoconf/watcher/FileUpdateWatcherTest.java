package com.github.autoconf.watcher;

import com.github.autoconf.api.IFileListener;
import com.github.autoconf.helper.ZookeeperUtil;
import com.google.common.io.Files;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试本地配置内容修改触发回调功能
 * Created by lirui on 2015-09-29 15:12.
 */
public class FileUpdateWatcherTest {
  private static final Logger LOG = LoggerFactory.getLogger(FileUpdateWatcherTest.class);

  @Test
  public void testListener() throws Exception {
    FileUpdateWatcher watcher = FileUpdateWatcher.getInstance();
    File d1 = Files.createTempDir();
    File f1 = d1.toPath().resolve("update.txt").toFile();
    //mac系统上获取回调通知特别慢,所以通过一个计数器来做忙等待.
    final AtomicInteger num = new AtomicInteger(0);
    IFileListener listener = new IFileListener() {
      @Override
      public void changed(Path path, byte[] content) {
        LOG.info("{} changed", path);
        num.incrementAndGet();
      }
    };
    try {
      watcher.watch(f1.toPath(), listener);
      //修改文件内容
      num.set(0);
      write(ZookeeperUtil.newBytes("a=1"), f1);
      busyWait(num);
      assertThat(num.get(), is(1));

      //本地修改有2s的屏蔽间隔,这里等待过期
      Thread.sleep(2000);

      //删除文件
      num.set(0);
      delete(f1);
      busyWait(num);
      assertThat(num.get(), is(1));
    } finally {
      delete(f1);
      delete(d1);
    }
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
    if (f == null || !f.exists())
      return;
    LOG.info("delete {}", f);
    if (!f.delete()) {
      f.deleteOnExit();
    }
  }
}
