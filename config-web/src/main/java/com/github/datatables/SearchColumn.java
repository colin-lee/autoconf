package com.github.datatables;

import com.google.common.base.Strings;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * 搜索字段
 *
 * Created by lirui on 15/2/8.
 */
public class SearchColumn implements Serializable {
  private String value;
  private boolean regex;
  private Pattern pattern;

  public SearchColumn(String value, boolean regex) {
    this.value = value;
    this.regex = regex;
    if (regex && !Strings.isNullOrEmpty(value)) {
      pattern = Pattern.compile(value, Pattern.CASE_INSENSITIVE);
    }
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean isRegex() {
    return regex;
  }

  public void setRegex(boolean regex) {
    this.regex = regex;
  }

  public boolean isSearchable() {
    return !Strings.isNullOrEmpty(value);
  }

  public boolean find(Object obj) {
    String s = String.valueOf(obj);
    if (regex) {
      return pattern.matcher(s).find();
    } else {
      return s.toLowerCase().contains(value);
    }
  }
}
