package com.github.autoconf;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试工具类
 * Created by lirui on 2015-10-03 11:04.
 */
public class TestHelper {
  private static final Logger LOG = LoggerFactory.getLogger(TestHelper.class);

  public static void busyWait(final AtomicInteger num) throws InterruptedException {
    int tries = 0;
    int millis = 1000;
    while (++tries < 60) {
      Thread.sleep(millis);
      LOG.info("delay {}s", tries);
      if (num.get() > 0) {
        return;
      }
    }
    LOG.error("detect timeout, delay {}s", tries);
  }

  public static void writeFile(byte[] bytes, File f) throws IOException {
    LOG.info("write {} bytes into {}", bytes.length, f);
    Files.write(bytes, f);
  }

  public static void deleteFile(File f) {
    if (f == null || !f.exists()) {
      return;
    }
    LOG.info("delete {}", f);
    if (!f.delete()) {
      f.deleteOnExit();
    }
  }
}
