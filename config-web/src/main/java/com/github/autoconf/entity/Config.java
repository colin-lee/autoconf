package com.github.autoconf.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.html.HtmlEscapers;
import org.hibernate.validator.constraints.Length;
import org.joda.time.DateTime;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置对象
 * Created by lirui on 15/1/10.
 */
public class Config {
  @Transient
  protected Function<String, String> escape = new Function<String, String>() {
    @Override
    public String apply(String input) {
      return HtmlEscapers.htmlEscaper().escape(input);
    }
  };

  private Long id;
  /**
   * 配置文件名
   */
  @Length(min = 2, max = 64)
  private String name;
  /**
   * 配置文件路径
   */
  private String path;
  /**
   * 配置组信息
   */
  @Length(min = 2, max = 128)
  private String profile;
  /**
   * 版本号
   */
  private int version;
  /**
   * 配置内容（摘要）
   */
  @Length(min = 1)
  private String content;
  /**
   * 谁修改了配置
   */
  private String editor;
  /**
   * 最后修改时间
   */
  @Transient
  private Date modifyTime;
  /**
   * 保存到zookeeper上需要使用的编码方式
   */
  private String encoding;
  /**
   * 查找的时候给出摘要信息
   */
  @Transient
  private String summary;

  public Config() {
  }

  public Config(String name, String profile, String content) {
    this.content = content;
    this.name = name;
    this.profile = profile;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonIgnore
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public void incVersion() {
    this.version++;
  }

  @JsonIgnore
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getEditor() {
    return editor;
  }

  public void setEditor(String editor) {
    this.editor = editor;
  }

  @JsonIgnore
  public Date getModifyTime() {
    return modifyTime;
  }

  public void setModifyTime(Date modifyTime) {
    this.modifyTime = modifyTime;
  }

  public String getMtime() {
    return new DateTime(modifyTime).toString("yyyy-MM-dd HH:mm:ss");
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public String getSummary() {
    if (summary == null) {
      List<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(content);
      List<String> items = lines.subList(0, Math.min(10, lines.size()));
      summary = Joiner.on("<br/>").join(Iterables.transform(items, escape));
    }
    return summary;
  }

  public void setSummary(List<String> s) {
    if (content != null) {
      List<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(content);
      List<String> head = Lists.newArrayList();
      if (!Iterables.isEmpty(s)) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (String i : s) {
          sb.append(CharMatcher.is('|').replaceFrom(i, "\\|")).append('|');
        }
        sb.setCharAt(sb.length() - 1, ')');
        Pattern p = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        int count = 0;
        for (String i : lines) {
          String n = highlight(i, p);
          if (n.length() > i.length()) {
            head.add(n);
            if (++count > 10)
              break;
          }
        }
        if (head.size() > 0) {
          summary = Joiner.on("<br/>").join(head);
        }
      } else {
        summary = null;
      }
    }
  }

  protected String highlight(String s, Pattern p) {
    Matcher m = p.matcher(s);
    StringBuilder sb = new StringBuilder();
    int start = 0;
    while (m.find()) {
      sb.append(HtmlEscapers.htmlEscaper().escape(s.substring(start, m.start(1))));
      sb.append("<span class=\"bg-danger text-blue\">").append(m.group(1)).append("</span>");
      start = m.end(1);
    }
    if (start < s.length()) {
      sb.append(HtmlEscapers.htmlEscaper().escape(s.substring(start)));
    }
    return sb.toString();
  }

  public boolean isNew() {
    return id == null;
  }

  @Override
  public String toString() {
    return "Config{" + "name=" + name + ',' + "profile=" + profile + ',' + "version=" + version + '}';
  }
}
