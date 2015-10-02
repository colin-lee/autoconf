package me.config.spring;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
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
  @Autowired
  private Article article;

  private void setVariable(String var) throws Exception {
    File app = ResourceUtils.getFile("classpath:autoconf/app.properties");
    List<String> lines = Lists.newArrayList();
    BufferedReader reader = new BufferedReader(new FileReader(app));
    String s;
    while ((s = reader.readLine()) != null) {
      if (s.startsWith("article.author")) {
        s = "article.author=" + var;
      }
      lines.add(s);
    }
    reader.close();
    FileOutputStream out = new FileOutputStream(app);
    out.write(Joiner.on("\r\n").join(lines).getBytes("UTF-8"));
    out.close();
    LoggerFactory.getLogger("PropertiesWatcher").info("======write app.properties=====");
  }

  @Test
  public void testPlaceHolder() throws Exception {
    setVariable("colinli");
    Thread.sleep(1000);
    assertThat(article.getAuthor(), is("colinli"));
    assertThat(article.getContent(), is("hello colinli, welcome"));
    assertThat(article.getTitle(), is("config title"));
    assertThat(article.getDynamic(), is("dynamicContent"));

    setVariable("lirui");
    Thread.sleep(10000);

    assertThat(article.getAuthor(), is("lirui"));
    assertThat(article.getContent(), is("hello lirui, welcome"));
    setVariable("colinli");
  }
}
