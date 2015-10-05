package com.github.autoconf.spring.reloadable;

import com.github.autoconf.spring.properties.bean.BeanPropertyHolder;
import com.github.autoconf.spring.properties.bean.DynamicProperty;
import com.github.autoconf.spring.properties.bean.PropertyModifiedEvent;
import com.github.autoconf.spring.properties.conversion.DefaultPropertyConversionService;
import com.github.autoconf.spring.properties.conversion.PropertyConversionService;
import com.github.autoconf.spring.properties.event.GuavaPropertyChangedEventNotifier;
import com.github.autoconf.spring.properties.event.PropertyChangedEventNotifier;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

/**
 * <p>
 * Processes beans on start up injecting field values marked with {@link ReloadableProperty} setting the associated annotated property value with properties
 * configured in a {@link ReloadablePropertySourcesPlaceholderConfigurer}.
 * </p>
 * <p>
 * The processor also has the ability to reload/re-inject properties from the configured {@link ReloadablePropertySourcesPlaceholderConfigurer} which are changed.
 * Once a property is reloaded the associated bean holding that value will have its property updated, no further bean operations are performed on the reloaded
 * bean.
 * </p>
 * <p>
 * The processor will also substitute any properties with values starting with "${" and ending with "}", none recursive.
 * </p>
 *
 * @author James Morgan
 */
@Component
public class ReloadablePropertyPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(ReloadablePropertyPostProcessor.class);
  private final PropertyChangedEventNotifier eventNotifier;
  private final PropertyConversionService propertyConversionService;
  private final ReloadablePropertySourcesPlaceholderConfigurer placeholderConfigurer;
  private Multimap<String, BeanPropertyHolder> beanPropertySubscriptions = HashMultimap.create();

  @Autowired
  public ReloadablePropertyPostProcessor(final ReloadablePropertySourcesPlaceholderConfigurer placeholderConfigurer) {
    this.propertyConversionService = new DefaultPropertyConversionService();
    this.placeholderConfigurer = placeholderConfigurer;
    this.eventNotifier = new GuavaPropertyChangedEventNotifier();
    this.placeholderConfigurer.setEventNotifier(this.eventNotifier);
  }

  @PostConstruct
  protected void init() {
    LOG.info("Registering ReloadablePropertyProcessor for properties file changes");
    registerPropertyReloader();
  }

  /**
   * Utility method to unregister the class from receiving events about property files being changed.
   */
  public final void unregisterPropertyReloader() {
    LOG.info("Unregistering ReloadablePropertyProcessor from property file changes");
    this.eventNotifier.unregister(this);
  }

  /**
   * Utility method to register the class for receiving events about property files being changed, setting up bean re-injection once triggered.
   */
  public final void registerPropertyReloader() {
    // Setup Guava event bus listener
    this.eventNotifier.register(this);
    // Trigger resource change listener
    this.placeholderConfigurer.startWatching();
  }

  /**
   * Method subscribing to the {@link PropertyModifiedEvent} utilising the {@link Subscribe} annotation
   *
   * @param event the {@link PropertyModifiedEvent} detailing what's changed
   */
  @Subscribe
  public void handlePropertyChange(final PropertyModifiedEvent event) {
    Collection<BeanPropertyHolder> c = beanPropertySubscriptions.get(event.getPropertyName());
    for (final BeanPropertyHolder bean : c) {
      updateField(bean, event);
    }
  }

  public void updateField(final BeanPropertyHolder holder, final PropertyModifiedEvent event) {
    final Object beanToUpdate = holder.getBean();
    final Field fieldToUpdate = holder.getField();
    final String rawValue = holder.getRawValue();
    final String canonicalName = beanToUpdate.getClass().getCanonicalName();

    final Object convertedProperty = convertPlaceHolderForField(fieldToUpdate, rawValue);
    try {
      LOG.info("Reloading property [{}] on field [{}] for class [{}]", event.getPropertyName(), fieldToUpdate.getName(), canonicalName);
      fieldToUpdate.set(beanToUpdate, convertedProperty);
    } catch (final IllegalAccessException e) {
      LOG.error("Unable to reloading property [{}] on field [{}] for class [{}]\n Exception [{}]", event.getPropertyName(), fieldToUpdate.getName(), canonicalName, e.getMessage());
    }
  }

  @Override
  public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {
    setPropertiesOnBean(bean);
    for (DynamicProperty property : placeholderConfigurer.getPlaceHolders().get(beanName)) {
      String name = property.getPropertyName();
      Field field = ReflectionUtils.findField(bean.getClass(), name);
      if (field != null) {
        ReflectionUtils.makeAccessible(field);
        validateFieldNotFinal(bean, field);
        for (String holder : property.getPlaceholders()) {
          BeanPropertyHolder bp = new BeanPropertyHolder(bean, field, property.getRawValue());
          subscribeBeanToPropertyChangedEvent(holder, bp);
        }
      } else {
        String methodName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        boolean found = false;
        for (Method i : bean.getClass().getMethods()) {
          if (i.getName().equals(methodName)) {
            found = true;
            Object value = placeholderConfigurer.resolvePlaceHolders(property.getRawValue());
            try {
              ReflectionUtils.makeAccessible(i);
              i.invoke(bean, value);
            } catch (Exception e) {
              LOG.error("cannot invoke {}.{}({})", bean.getClass(), methodName, value);
            }
            break;
          }
        }
        if (!found) {
          LOG.error("cannot find {} in class: {}", methodName, bean.getClass());
        }
      }
    }
    return true;
  }

  private void setPropertiesOnBean(final Object bean) {
    ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
      @Override
      public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
        final ReloadableProperty annotation = field.getAnnotation(ReloadableProperty.class);
        if (null != annotation) {
          ReflectionUtils.makeAccessible(field);
          validateFieldNotFinal(bean, field);

          final Object property = placeholderConfigurer.resolveProperty(annotation.value());
          validatePropertyAvailableOrDefaultSet(bean, field, annotation, property);
          if (null != property) {
            final Object convertedProperty = convertPropertyForField(field, annotation.value());
            LOG.info("Setting field [{}] of class [{}] with value [{}]", field.getName(), bean.getClass().getCanonicalName(), convertedProperty);
            field.set(bean, convertedProperty);
            subscribeBeanToPropertyChangedEvent(annotation.value(), new BeanPropertyHolder(bean, field, annotation.value()));
          } else {
            LOG.info("Leaving field [{}] of class [{}] with default value", field.getName(), bean.getClass().getCanonicalName());
          }
        }
      }
    });
  }

  private void validatePropertyAvailableOrDefaultSet(final Object bean, final Field field, final ReloadableProperty annotation, final Object property) throws IllegalArgumentException, IllegalAccessException {
    if (null == property && fieldDoesNotHaveDefault(field, bean)) {
      throw new BeanInitializationException(String.format(
        "No property found for field annotated with @ReloadableProperty, "
          + "and no default specified. Property [%s] of class [%s] requires a property named [%s]", field.getName(), bean.getClass().getCanonicalName(), annotation.value()));
    }
  }

  private void validateFieldNotFinal(final Object bean, final Field field) {
    if (Modifier.isFinal(field.getModifiers())) {
      throw new BeanInitializationException(String.format("Unable to set field [%s] of class [%s] as is declared final", field.getName(), bean.getClass().getCanonicalName()));
    }
  }

  private boolean fieldDoesNotHaveDefault(final Field field, final Object value) throws IllegalArgumentException, IllegalAccessException {
    try {
      return (null == field.get(value));
    } catch (final NullPointerException e) {
      return true;
    }
  }

  private void subscribeBeanToPropertyChangedEvent(final String property, final BeanPropertyHolder fieldProperty) {
    this.beanPropertySubscriptions.put(property, fieldProperty);
  }

  // ///////////////////////////////////
  // Utility methods for class access //
  // ///////////////////////////////////
  private Object convertPlaceHolderForField(final Field field, final String text) {
    return this.propertyConversionService.convertPropertyForField(field, this.placeholderConfigurer.resolvePlaceHolders(text));
  }

  private Object convertPropertyForField(final Field field, final Object property) {
    return this.propertyConversionService.convertPropertyForField(field, this.placeholderConfigurer.resolveProperty(property));
  }
}
