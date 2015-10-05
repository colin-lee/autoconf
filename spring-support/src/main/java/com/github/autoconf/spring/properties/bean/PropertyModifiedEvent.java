package com.github.autoconf.spring.properties.bean;

import com.google.common.base.Objects;

public class PropertyModifiedEvent {

  private final String propertyName;
  private final Object oldValue;
  private final Object newValue;

  public PropertyModifiedEvent(final String propertyName, final Object oldValue, final Object newValue) {
    this.propertyName = propertyName;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public String getPropertyName() {
    return this.propertyName;
  }

  public Object getOldValue() {
    return this.oldValue;
  }

  public Object getNewValue() {
    return this.newValue;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.propertyName, this.oldValue, this.newValue);
  }

  @Override
  public boolean equals(final Object object) {
    if (object instanceof PropertyModifiedEvent) {
      final PropertyModifiedEvent that = (PropertyModifiedEvent) object;
      return Objects.equal(this.propertyName, that.propertyName)
        && Objects.equal(this.oldValue, that.oldValue)
        && Objects.equal(this.newValue, that.newValue);
    }
    return false;
  }

  @Override
  public String toString() {
    return "PropertyModifiedEvent{" +
      "propertyName='" + propertyName + '\'' +
      ", oldValue=" + oldValue +
      ", newValue=" + newValue +
      '}';
  }
}
