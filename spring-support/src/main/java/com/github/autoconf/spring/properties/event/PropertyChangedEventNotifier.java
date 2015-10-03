package com.github.autoconf.spring.properties.event;

import com.github.autoconf.spring.properties.bean.PropertyModifiedEvent;
import com.github.autoconf.spring.reloadable.ReloadablePropertyPostProcessor;

public interface PropertyChangedEventNotifier {

  void post(PropertyModifiedEvent propertyChangedEvent);

  void unregister(ReloadablePropertyPostProcessor reloadablePropertyProcessor);

  void register(ReloadablePropertyPostProcessor reloadablePropertyProcessor);

}
