package com.github.autoconf.web;

import com.github.autoconf.mapper.UserMapper;
import com.github.autoconf.spring.reloadable.ReloadableProperty;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 测试配置加载
 * Created by lirui on 2015-10-05 22:28.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:mysql-druid.xml"})
public class ConfigTest {
  @ReloadableProperty("mysql.username")
  private String username;

  @Autowired
  private UserMapper mapper;

  @Test
  public void testLoadConfig() throws Exception {
    assertThat(username, is("root"));
    mapper.findByUserName("lirui");
  }
}
