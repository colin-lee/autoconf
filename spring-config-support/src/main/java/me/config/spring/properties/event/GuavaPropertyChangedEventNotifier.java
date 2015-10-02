package me.config.spring.properties.event;

import com.google.common.eventbus.EventBus;
import me.config.spring.properties.bean.PropertyModifiedEvent;
import me.config.spring.reloadable.ReloadablePropertyPostProcessor;
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
