package com.github.autoconf.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 测试配置文件加载
 * Created by lirui on 2015-10-06 10:58.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class LoaderTest {
  @Test
  public void testLoadContext() throws Exception {
  }
}
