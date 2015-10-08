package com.github.autoconf.base;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * 测试获取各种类型数据
 * Created by lirui on 2015-09-28 17:59.
 */
public class PropertiesTest {
  @Test
  public void testDefault() throws Exception {
    Properties m = new Properties();
    String s = "-";
    assertThat(m.get(s), nullValue());
    assertThat(m.getInt(s), is(0));
    assertThat(m.getLong(s), is(0L));
    assertThat(m.getDouble(s), is(0.0));
    assertThat(m.getBool(s), is(false));
  }

  @Test
  public void testDefault2() throws Exception {
    Properties m = new Properties();
    String s = "-";
    assertThat(m.get(s, "1"), is("1"));
    assertThat(m.getInt(s, 1), is(1));
    assertThat(m.getLong(s, 1L), is(1L));
    assertThat(m.getDouble(s, 1.0), is(1.0));
    assertThat(m.getBool(s, true), is(true));
  }

  @Test
  public void testGetType() throws Exception {
    Properties m = new Properties();
    Map<String, String> raw = ImmutableMap.of("int", "1", "long", "2", "bool", "TRUE", "double", "3.6", "s", "a string");
    m.copyOf(raw);
    assertThat(m.has("_"), is(false));
    assertThat(m.get("s"), is("a string"));
    assertThat(m.getInt("int"), is(1));
    assertThat(m.getLong("long"), is(2L));
    assertThat(m.getBool("bool"), is(true));
    assertThat(m.getDouble("double"), is(3.6));
    assertThat(m.getAll(), equalTo(raw));
  }

  @Test
  public void testParseException() throws Exception {
    Properties m = new Properties();
    Map<String, String> raw = ImmutableMap.of("int", "s1", "long", "s2", "bool", "sTRUE", "double", "s3.6");
    m.copyOf(raw);
    assertThat(m.getInt("int"), is(0));
    assertThat(m.getLong("long"), is(0L));
    assertThat(m.getDouble("bool"), is(0.0));
    assertThat(m.getBool("double"), is(false));
  }
}
