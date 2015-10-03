package com.github.autoconf.spring.properties.event;

import com.github.autoconf.spring.properties.bean.PropertyModifiedEvent;
import com.github.autoconf.spring.reloadable.ReloadablePropertyPostProcessor;
import com.google.common.eventbus.EventBus;
import org.springframework.stereotype.Component;

@Component
public class GuavaPropertyChangedEventNotifier implements PropertyChangedEventNotifier {

  private final EventBus eventBus = new EventBus("propertiesEventBus");

  @Override
  public void post(final PropertyModifiedEvent propertyChangedEvent) {
    this.eventBus.post(propertyChangedEvent);
  }

  @Override
  public void unregister(final ReloadablePropertyPostProcessor ReloadablePropertyPostProcessor) {
    this.eventBus.unregister(ReloadablePropertyPostProcessor);
  }

  @Override
  public void register(final ReloadablePropertyPostProcessor ReloadablePropertyPostProcessor) {
    this.eventBus.register(ReloadablePropertyPostProcessor);
  }

}
