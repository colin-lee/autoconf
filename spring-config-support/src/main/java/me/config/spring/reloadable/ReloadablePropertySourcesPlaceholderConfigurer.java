package me.config.spring.reloadable;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import me.config.ConfigFactory;
import me.config.api.IChangeListener;
import me.config.api.IChangeableConfig;
import me.config.api.IConfig;
import me.config.spring.properties.bean.DynamicProperty;
import me.config.spring.properties.bean.PropertyModifiedEvent;
import me.config.spring.properties.event.PropertyChangedEventNotifier;
import me.config.spring.properties.internal.PropertiesWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Specialisation of {@link PropertySourcesPlaceholderConfigurer} that can react to changes in the resources specified. The watching process does not start by
 * default, initiation is triggered by calling <code>ReadablePropertySourcesPlaceholderConfigurer.startWatching()</code>
 *
 * @author James Morgan
 */
public class ReloadablePropertySourcesPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer implements PropertiesWatcher.EventPublisher {
  private Logger log =
    LoggerFactory.getLogger(ReloadablePropertySourcesPlaceholderConfigurer.class);
  private PropertyChangedEventNotifier eventNotifier;
  private Resource[] locations;
  private Multimap<String, DynamicProperty> placeHolders = HashMultimap.create();
  private PropertySourcesPropertyResolver propertyResolver;
  /**
   * 在cms系统中配置的名称，可以是逗号分隔的多个名字
   */
  private String configName;
  /**
   * the application context is needed to find the beans again during reconfiguration
   */
  private BeanFactory beanFactory;
  private String beanName;

  public ReloadablePropertySourcesPlaceholderConfigurer() {
  }

  void setEventNotifier(PropertyChangedEventNotifier eventNotifier) {
    this.eventNotifier = eventNotifier;
  }

  public void setConfigName(String configName) {
    this.configName = configName;
  }

  @Override
  protected void loadProperties(final Properties props) throws IOException {
    super.loadProperties(props);
    if (!Strings.isNullOrEmpty(configName)) {
      IChangeableConfig config =
        ConfigFactory.getInstance().getConfig(configName, new IChangeListener() {
          @Override
          public void changed(IConfig config) {
            log.info("CMS: {} changed", config.getName());
            reloadCmsConfig(config);
          }
        }, false);
      props.putAll(config.getAll());
    }
  }

  private void reloadCmsConfig(IConfig config) {
    final Properties reloadedProperties = new Properties();
    reloadedProperties.putAll(config.getAll());
    updateProperty(reloadedProperties);
  }

  private void updateProperty(Properties reloadedProperties) {
    PropertySources appliedSources = getAppliedPropertySources();
    MutablePropertySources oldSources = new MutablePropertySources();
    PropertiesPropertySource localPropertySource = null;
    for (PropertySource i : appliedSources) {
      if (i.getName().equals(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME)) {
        localPropertySource = (PropertiesPropertySource) i;
        Properties old = new Properties();
        old.putAll(localPropertySource.getSource());
        oldSources.addLast(new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, old));
        for (String name : reloadedProperties.stringPropertyNames()) {
          localPropertySource.getSource().put(name, reloadedProperties.get(name));
        }
      } else {
        oldSources.addLast(i);
      }
    }

    if (localPropertySource != null) {
      PropertySourcesPropertyResolver oldResolver = buildPropertyResolver(oldSources);
      PropertySourcesPropertyResolver newResolver = buildPropertyResolver(appliedSources);

      for (final String property : localPropertySource.getPropertyNames()) {
        final String oldValue = oldResolver.getProperty(property);
        final String newValue = newResolver.getProperty(property);

        boolean propertyExistsAndNotNull =
          localPropertySource.containsProperty(property) && null != newValue;
        boolean propertyChange = null == oldValue || !oldValue.equals(newValue);
        if (propertyExistsAndNotNull && propertyChange) {
          // Post change event to notify any potential listeners
          this.eventNotifier.post(new PropertyModifiedEvent(property, oldValue, newValue));
        }
      }
    }
  }

  @Override
  public void setLocations(final Resource[] locations) {
    super.setLocations(locations);
    this.locations = locations;
  }

  @Override
  public void onResourceChanged(final Resource resource) {
    try {
      updateProperty(PropertiesLoaderUtils.loadProperties(resource));
    } catch (final IOException e) {
      log.error("Failed to reload properties file once change", e);
    }
  }

  public void startWatching() {
    if (null == this.eventNotifier) {
      throw new BeanInitializationException("Event bus not setup, you should not be calling this method...!");
    }
    if (this.locations != null) {
      try {
        // Here we actually create and set a FileWatcher to monitor the given locations
        Executors.newSingleThreadExecutor().execute(new PropertiesWatcher(this.locations, this));
      } catch (final IOException e) {
        log.error("Unable to start properties file watcher", e);
      }
    }
  }

  public PropertySourcesPropertyResolver getPropertyResolver() {
    if (propertyResolver == null) {
      propertyResolver = buildPropertyResolver(getAppliedPropertySources());
    }
    return propertyResolver;
  }

  public Object resolvePlaceHolders(final String text) {
    return getPropertyResolver().resolvePlaceholders(text);
  }

  public Object resolveProperty(final Object property) {
    return getPropertyResolver().getProperty(property.toString());
  }

  /**
   * Only necessary to check that we're not parsing our own bean definition,
   * to avoid failing on unresolvable placeholders in properties file locations.
   * The latter case can happen with placeholders for system properties in
   * resource locations.
   *
   * @see #setLocations
   * @see org.springframework.core.io.ResourceEditor
   */
  @Override
  public void setBeanName(String beanName) {
    super.setBeanName(beanName);
    this.beanName = beanName;
  }

  /**
   * Only necessary to check that we're not parsing our own bean definition,
   * to avoid failing on unresolvable placeholders in properties file locations.
   * The latter case can happen with placeholders for system properties in
   * resource locations.
   *
   * @see #setLocations
   * @see org.springframework.core.io.ResourceEditor
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    this.beanFactory = beanFactory;
  }

  private PropertySourcesPropertyResolver buildPropertyResolver(PropertySources sources) {
    PropertySourcesPropertyResolver propertyResolver = new PropertySourcesPropertyResolver(sources);
    propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
    propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
    propertyResolver.setValueSeparator(this.valueSeparator);
    return propertyResolver;
  }

  protected void doProcessProperties(ConfigurableListableBeanFactory beanFactoryToProcess, StringValueResolver valueResolver) {
    BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(valueResolver);
    String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
    for (String curName : beanNames) {
      // Check that we're not parsing our own bean definition,
      // to avoid failing on unresolvable placeholders in properties file locations.
      if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
        BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
        try {
          findDynamicProperties(curName, bd);
          visitor.visitBeanDefinition(bd);
        } catch (Exception ex) {
          throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
        }
      }
    }

    // New in Spring 2.5: resolve placeholders in alias target names and aliases as well.
    beanFactoryToProcess.resolveAliases(valueResolver);

    // New in Spring 3.0: resolve placeholders in embedded values such as annotation attributes.
    beanFactoryToProcess.addEmbeddedValueResolver(valueResolver);
  }

  private void findDynamicProperties(String beanName, BeanDefinition bd) {
    MutablePropertyValues pvs = bd.getPropertyValues();
    PropertyValue[] pvArray = pvs.getPropertyValues();
    for (PropertyValue pv : pvArray) {
      Object value = pv.getValue();
      if (value instanceof TypedStringValue) {
        String rawValue = ((TypedStringValue) value).getValue();
        List<String> holders = findHolders(rawValue);
        if (holders != null && holders.size() > 0) {
          DynamicProperty p = new DynamicProperty(beanName, pv.getName(), rawValue);
          p.setPlaceholders(holders);
          placeHolders.put(beanName, p);
        }
      }
    }
  }

  private List<String> findHolders(String rawValue) {
    if (Strings.isNullOrEmpty(rawValue))
      return null;
    int start = rawValue.indexOf(this.placeholderPrefix);
    if (start == -1)
      return null;
    List<String> holders = Lists.newArrayList();
    while (start != -1) {
      start += this.placeholderPrefix.length();
      int stop = rawValue.indexOf(this.placeholderSuffix, start);
      if (stop == -1)
        break;
      holders.add(rawValue.substring(start, stop));
      start = rawValue.indexOf(this.placeholderPrefix, stop + this.placeholderSuffix.length());
    }
    return holders;
  }

  public Multimap<String, DynamicProperty> getPlaceHolders() {
    return placeHolders;
  }
}
