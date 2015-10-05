package com.github.autoconf.spring;

import com.github.autoconf.spring.reloadable.ReloadableProperty;
import org.springframework.beans.factory.annotation.Value;

/**
 * 测试类注入
 * <p/>
 * Created by lirui on 2015/2/26.
 */
public class Article {
  private String author;
  private String content;
  @Value("${article.title:default title}")
  private String title;
  @ReloadableProperty("dynamic")
  private String dynamic = "default";

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDynamic() {
    return dynamic;
  }

  public void setDynamic(String dynamic) {
    this.dynamic = dynamic;
  }

  @Override
  public String toString() {
    return "Article{" +
      "author='" + author + '\'' +
      ", content='" + content + '\'' +
      ", title='" + title + '\'' +
      ", dynamic='" + dynamic + '\'' +
      '}';
  }
}
