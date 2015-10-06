package com.github.autoconf.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 测试cache配置加载
 * Created by lirui on 2015-10-06 10:46.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:guava-cache.xml"})
public class CacheTest {
  @Test
  public void testCacheSpec() throws Exception {
  }
}
