package com.github.autoconf.base;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * 测试纯文本配置
 * Created by lirui on 2015-09-28 20:12.
 */
public class ConfigTest {
  @Test
  public void testTextFile() throws Exception {
    String s = "<root>\n<item name=\"colin\"/>\n</root>\n";
    Config c = new Config();
    c.copyOf(s);
    List<String> lines = c.getLines();
    assertThat(c.getContent(), equalTo(s.getBytes("UTF-8")));
    assertThat(c.getString(), is(s));
    assertThat(lines.size(), is(3));
    assertThat(lines.get(2), is("</root>"));
  }
}
