package com.github.autoconf.impl;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 本地文件配置
 * Created by lirui on 2015-09-28 19:55.
 */
public class LocalConfigTest {
  @Test
  public void testLocalFile() throws Exception {
    File tempFile = File.createTempFile(".test", ".ini");
    try {
      byte[] s = " int = 1  \t\n#comment\n long= 2\n".getBytes();
      Files.write(s, tempFile);
      LocalConfig local = new LocalConfig("test", tempFile.toPath());
      assertThat(local.getInt("int"), is(1));
      assertThat(local.getLong("long"), is(2L));
    } finally {
      if (!tempFile.delete())
        tempFile.deleteOnExit();
    }
  }
}
