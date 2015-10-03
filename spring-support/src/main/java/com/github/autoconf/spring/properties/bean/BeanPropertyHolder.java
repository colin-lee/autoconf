package com.github.autoconf.spring.properties.bean;

import com.google.common.base.Objects;

import java.lang.reflect.Field;

public class BeanPropertyHolder {

  private final Object bean;
  private final Field field;
  private final String rawValue;

  public BeanPropertyHolder(Object bean, Field field, String rawValue) {
    this.bean = bean;
    this.field = field;
    this.rawValue = rawValue;
  }

  public Object getBean() {
    return this.bean;
  }

  public Field getField() {
    return this.field;
  }

  public String getRawValue() {
    return rawValue;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.bean, this.field);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof BeanPropertyHolder) {
      BeanPropertyHolder that = (BeanPropertyHolder) object;
      return Objects.equal(this.bean, that.bean) && Objects.equal(this.field, that.field);
    }
    return false;
  }

  @Override
  public String toString() {
    return "BeanPropertyHolder{" + "bean=" + bean + ", field=" + field + ", rawValue='" + rawValue
      + '\'' + '}';
  }
}
