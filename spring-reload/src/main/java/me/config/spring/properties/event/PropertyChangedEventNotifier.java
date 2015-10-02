package me.config.spring.properties.event;

import me.config.spring.properties.bean.PropertyModifiedEvent;
import me.config.spring.reloadable.ReloadablePropertyPostProcessor;

public interface PropertyChangedEventNotifier {

  void post(PropertyModifiedEvent propertyChangedEvent);

  void unregister(ReloadablePropertyPostProcessor reloadablePropertyProcessor);

  void register(ReloadablePropertyPostProcessor reloadablePropertyProcessor);

}
