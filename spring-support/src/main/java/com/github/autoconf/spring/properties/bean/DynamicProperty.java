package com.github.autoconf.spring.properties.bean;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 动态属性meta信息
 * Created by lirui on 2015/02/27 上午10:23.
 */
public class DynamicProperty {
  final String beanName;
  final String propertyName;
  final String rawValue;
  List<String> placeholders = Lists.newArrayList();

  public DynamicProperty(String beanName, String propertyName, String rawValue) {
    this.beanName = beanName;
    this.propertyName = propertyName;
    this.rawValue = rawValue;
  }

  public List<String> getPlaceholders() {
    return placeholders;
  }

  public void setPlaceholders(List<String> placeholders) {
    this.placeholders = placeholders;
  }

  public String getRawValue() {
    return rawValue;
  }

  public String getBeanName() {
    return beanName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  @SuppressWarnings("RedundantIfStatement")
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final DynamicProperty that = (DynamicProperty) o;

    if (beanName != null ? !beanName.equals(that.beanName) : that.beanName != null) {
      return false;
    }
    if (propertyName != null ?
      !propertyName.equals(that.propertyName) :
      that.propertyName != null) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    int result;
    result = (beanName != null ? beanName.hashCode() : 0);
    result = 29 * result + (propertyName != null ? propertyName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "DynamicProperty{" + "beanName='" + beanName + '\'' + ", propertyName='" + propertyName
      + '\'' + ", rawValue='" + rawValue + '\'' + ", placeholders=" + placeholders + '}';
  }
}
