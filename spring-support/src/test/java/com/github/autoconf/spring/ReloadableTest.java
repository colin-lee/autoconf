package com.github.autoconf.spring;

import com.github.autoconf.base.Config;
import com.github.autoconf.spring.reloadable.ReloadableProperty;
import com.google.common.io.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * 测试用例
 * <p/>
 * Created by lirui on 15/2/26.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class ReloadableTest {
  @ReloadableProperty("log4j.appender.Console")
  private String appender;

  @Autowired
  private Article article;

  private void setVariable(String var) throws Exception {
    File app = ResourceUtils.getFile("classpath:autoconf/app.properties");
    List<String> lines = Files.readLines(app, Config.UTF8);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(app)));
    for (String s : lines) {
      if (s.startsWith("article.author")) {
        s = "article.author=" + var;
      }
      writer.write(s);
      writer.write('\n');
    }
    writer.close();
  }

  @Test
  public void testPlaceHolder() throws Exception {
    assertThat(appender, is("org.apache.log4j.ConsoleAppender"));

    setVariable("colinli");
    Thread.sleep(3000);
    assertThat(article.getAuthor(), is("colinli"));
    assertThat(article.getContent(), is("hello colinli, welcome"));
    assertThat(article.getTitle(), is("config title"));
    assertThat(article.getDynamic(), is("dynamicContent"));

    setVariable("lirui");
    Thread.sleep(3000);

    assertThat(article.getAuthor(), is("lirui"));
    assertThat(article.getContent(), is("hello lirui, welcome"));
    setVariable("colinli");
  }
}
